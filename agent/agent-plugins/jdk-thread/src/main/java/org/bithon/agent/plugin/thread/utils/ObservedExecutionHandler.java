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
import org.bithon.agent.observability.tracing.context.TraceSpanFactory;
import org.bithon.agent.plugin.thread.metrics.ThreadPoolMetricRegistry;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * A wrapper to {@link RejectedExecutionHandler} to support tracing and metrics
 *
 * @author frank.chen021@outlook.com
 * @date 2023/3/23 21:07
 */
public class ObservedExecutionHandler implements RejectedExecutionHandler {
    private final RejectedExecutionHandler delegate;

    public ObservedExecutionHandler(RejectedExecutionHandler delegate) {
        this.delegate = delegate;
    }

    @Override
    public void rejectedExecution(Runnable task, ThreadPoolExecutor executor) {
        if (task instanceof ObservedTask) {
            // Restore the runnable object for user's application code
            task = ((ObservedTask) task).getTask();
        }

        Throwable throwable = null;
        ITraceSpan span = before();
        try {
            delegate.rejectedExecution(task, executor);
        } catch (Throwable e) {
            throwable = e;
            throw e;
        } finally {
            after(executor, span, throwable);
        }
    }

    private ITraceSpan before() {
        ITraceSpan span = TraceSpanFactory.newSpan("threadPool");
        if (span != null) {
            span.method(delegate.getClass().getName(), "rejectedExecution");
        }
        return span;
    }

    private void after(ThreadPoolExecutor executor, ITraceSpan span, Throwable e) {
        if (span != null) {
            span.tag(e).finish();
        }

        switch (delegate.getClass().getName()) {
            case "java.util.concurrent.ThreadPoolExecutor$AbortPolicy":
                ThreadPoolMetricRegistry.getInstance().addAbort(executor);
                break;

            case "java.util.concurrent.ThreadPoolExecutor$DiscardPolicy":
                ThreadPoolMetricRegistry.getInstance().addDiscard(executor);
                break;

            case "java.util.concurrent.ThreadPoolExecutor$CallerRunsPolicy":
                ThreadPoolMetricRegistry.getInstance().addCallerRun(executor);
                break;

            case "java.util.concurrent.ThreadPoolExecutor$DiscardOldestPolicy":
                ThreadPoolMetricRegistry.getInstance().addDiscardOldest(executor);
                break;

            default:
                ThreadPoolMetricRegistry.getInstance().addUserPolicy(executor);
                break;
        }
    }
}
