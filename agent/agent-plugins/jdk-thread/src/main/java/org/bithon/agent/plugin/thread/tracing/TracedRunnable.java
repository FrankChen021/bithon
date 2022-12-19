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

package org.bithon.agent.plugin.thread.tracing;

import org.bithon.agent.core.tracing.context.ITraceSpan;
import org.bithon.agent.core.tracing.context.TraceContextHolder;

/**
 * @author frank.chen021@outlook.com
 * @date 12/5/22 8:57 PM
 */
public class TracedRunnable implements Runnable {
    private final Runnable delegate;
    private final ITraceSpan runnableSpan;

    public TracedRunnable(Runnable delegate, ITraceSpan runnableSpan) {
        this.delegate = delegate;
        this.runnableSpan = runnableSpan;
    }

    @Override
    public void run() {
        // Setup context on current thread
        TraceContextHolder.set(runnableSpan.context());

        runnableSpan.start();

        try {
            delegate.run();
        } catch (Throwable e) {
            runnableSpan.tag(e);
            throw e;
        } finally {
            // Set the thread at the end because the thread name might be updated in the users' runnable
            runnableSpan.tag("thread", Thread.currentThread().getName())
                        .finish();
            runnableSpan.context().finish();

            // Clear context on current thread
            TraceContextHolder.remove();
        }
    }
}
