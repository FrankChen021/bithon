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

package org.bithon.agent.plugin.thread.threadpool;

import org.bithon.agent.core.tracing.context.ITraceSpan;
import org.bithon.agent.core.tracing.context.TraceContextHolder;
import org.bithon.agent.core.tracing.context.TraceSpanFactory;

/**
 * @author frank.chen021@outlook.com
 * @date 12/5/22 8:57 PM
 */
public class TracedRunnable implements Runnable {
    private final Runnable delegate;
    private final ITraceSpan span;

    public TracedRunnable(Runnable delegate) {
        this.delegate = delegate;
        this.span = TraceSpanFactory.newAsyncSpan("task")
                                    .clazz(delegate.getClass().getName())
                                    .method("run");
    }

    @Override
    public void run() {
        if (!span.isNull()) {
            TraceContextHolder.set(span.context());
            span.start();
        }
        try {
            delegate.run();
        } finally {
            if (!span.isNull()) {
                span.tag("thread", Thread.currentThread().getName())
                    .finish();
                span.context().finish();
                TraceContextHolder.remove();
            }
        }
    }
}
