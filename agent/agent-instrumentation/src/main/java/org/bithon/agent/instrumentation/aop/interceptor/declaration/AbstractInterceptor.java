/*
 *    Copyright 2020 bithon.org
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.bithon.agent.instrumentation.aop.interceptor.declaration;

import org.bithon.agent.instrumentation.logging.ILogger;
import org.bithon.agent.instrumentation.logging.LoggerFactory;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/3/18 23:29
 */
public abstract class AbstractInterceptor {
    private static final ILogger LOG = LoggerFactory.getLogger(AbstractInterceptor.class);

    private final LongAdder hitCount = new LongAdder();
    private long lastHitTime;

    // Statistics for exceptions thrown from the interceptor
    private long exceptionCount;
    private long lastExceptionTime;
    private Throwable lastException;

    // For exception logging control
    private final Map<String, Long> lastExceptionInfo = new HashMap<>();

    public long getHitCount() {
        return hitCount.sum();
    }

    public long getExceptionCount() {
        return exceptionCount;
    }

    public long getLastExceptionTime() {
        return lastExceptionTime;
    }

    public Throwable getLastException() {
        return lastException;
    }

    public long getLastHitTime() {
        return lastHitTime;
    }

    public void hit() {
        hitCount.increment();
        lastHitTime = System.currentTimeMillis();
    }

    /**
     * Called when an exception occurs during the execution of the interceptor.
     * This method handles exception counting, logging, and rate limiting to avoid log flooding.
     *
     * @param throwable the exception that occurred, can be nullable
     * @param phase     the phase during which the exception occurred (e.g., "before", "after")
     */
    public void onException(Throwable throwable, String phase) {
        if (throwable == null) {
            return;
        }

        synchronized (this) {
            long currentTime = System.currentTimeMillis();

            this.lastExceptionTime = currentTime;
            this.lastException = throwable;
            this.exceptionCount++;

            // Rate limiting: avoid flooding logs with the same exception type
            String exceptionType = throwable.getClass().getName();
            Long lastLogTime = lastExceptionInfo.get(exceptionType);
            if (lastLogTime != null && (currentTime - lastLogTime) < 30_000) {
                // Same exception type logged within 30 seconds, skip logging
                return;
            } else {
                lastExceptionInfo.put(exceptionType, currentTime);
            }
        }

        // Log the exception outside synchronized block to avoid holding lock during I/O
        LOG.warn(String.format(Locale.ENGLISH,
                               "Exception occurred when executing %s of interceptor [%s]: %s",
                               phase,
                               this.getClass().getSimpleName(),
                               throwable.getMessage()),
                 throwable);
    }

    /**
     * Convenience method called when an exception occurs during the "before" phase of interception.
     *
     * @param throwable the exception that occurred
     */
    public void onBeforeException(Throwable throwable) {
        onException(throwable, "before");
    }

    /**
     * Convenience method called when an exception occurs during the "after" phase of interception.
     *
     * @param throwable the exception that occurred
     */
    public void onAfterException(Throwable throwable) {
        onException(throwable, "after");
    }
}
