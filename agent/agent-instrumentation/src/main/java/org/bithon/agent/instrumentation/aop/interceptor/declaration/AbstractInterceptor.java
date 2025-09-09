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

import java.util.concurrent.atomic.LongAdder;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/3/18 23:29
 */
public abstract class AbstractInterceptor {
    private final LongAdder hitCount = new LongAdder();

    private long lastHitTime;

    // Statistics for exceptions thrown from the interceptor
    private long exceptionCount;
    private long lastExceptionTime;
    private Throwable lastException;

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

    public synchronized void exception(Throwable throwable) {
        exceptionCount++;
        lastExceptionTime = System.currentTimeMillis();
        lastException = throwable;
    }
}
