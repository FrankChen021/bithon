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

package org.bithon.agent.plugin.thread.utils;

import org.bithon.agent.observability.tracing.context.ITraceSpan;
import org.bithon.agent.observability.tracing.context.TraceContextHolder;
import org.bithon.agent.plugin.thread.metrics.ThreadPoolMetricRegistry;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author frank.chen021@outlook.com
 * @date 12/5/22 8:57 PM
 */
public class ObservableRunnable implements Runnable {
    private final ThreadPoolExecutor executor;
    private final Runnable delegate;
    private final ITraceSpan runnableSpan;

    public ObservableRunnable(ThreadPoolExecutor executor,
                              Runnable delegate,
                              ITraceSpan runnableSpan) {
        this.executor = executor;
        this.delegate = delegate;
        this.runnableSpan = runnableSpan;
    }

    public Runnable getDelegate() {
        return delegate;
    }

    @Override
    public void run() {
        if (this.runnableSpan == null) {
            runWithoutTracing();
        } else {
            runWithTracing();
        }
    }

    private void runWithTracing() {
        boolean hasException = false;

        // Setup context on current thread
        TraceContextHolder.set(runnableSpan.context());

        runnableSpan.start();

        try {
            delegate.run();
        } catch (Throwable e) {
            hasException = true;
            runnableSpan.tag(e);
            throw e;
        } finally {
            // Set the thread at the end because the thread name might be updated in the users' runnable
            runnableSpan.tag("thread", Thread.currentThread().getName())
                        .finish();
            runnableSpan.context().finish();

            // Clear context on current thread
            TraceContextHolder.remove();

            ThreadPoolMetricRegistry.getInstance().addRunCount(executor,
                                                               runnableSpan.endTime() - runnableSpan.startTime(),
                                                               hasException);
        }
    }

    private void runWithoutTracing() {
        boolean hasException = false;

        long millis = System.currentTimeMillis();
        long nanos = System.nanoTime();

        try {
            delegate.run();
        } catch (Exception e) {
            hasException = true;
            throw e;
        } finally {
            ThreadPoolMetricRegistry.getInstance().addRunCount(executor,
                                                               (System.nanoTime() - nanos) / 1000L + (System.currentTimeMillis() - millis) * 1000,
                                                               hasException);
        }
    }
}
