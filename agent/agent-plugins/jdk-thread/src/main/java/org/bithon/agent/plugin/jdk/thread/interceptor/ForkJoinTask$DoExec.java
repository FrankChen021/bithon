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

package org.bithon.agent.plugin.jdk.thread.interceptor;

import org.bithon.agent.instrumentation.aop.IBithonObject;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.InterceptionDecision;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AroundInterceptor;
import org.bithon.agent.observability.tracing.context.ITraceSpan;
import org.bithon.agent.observability.tracing.context.TraceContextHolder;
import org.bithon.agent.plugin.jdk.thread.metrics.ThreadPoolMetricRegistry;
import org.bithon.component.commons.tracing.Tags;

/**
 * Restore tracing context for the current task before {@link java.util.concurrent.ForkJoinTask#doExec()}
 *
 * @author frank.chen021@outlook.com
 * @date 2024/12/23 22:34
 */
public class ForkJoinTask$DoExec extends AroundInterceptor {

    @Override
    public InterceptionDecision before(AopContext aopContext) {
        IBithonObject forkJoinTask = (IBithonObject) aopContext.getTarget();

        // task context is injected in the ForkJoinTask ctor
        ForkJoinTaskContext asyncTaskContext = (ForkJoinTaskContext) forkJoinTask.getInjectedObject();
        if (asyncTaskContext != null
            // The Span is injected by ForkJoinPool$ExternalPush
            && asyncTaskContext.rootSpan != null) {

            TraceContextHolder.attach(asyncTaskContext.rootSpan.context());
            aopContext.setSpan(asyncTaskContext.rootSpan.start());
        }

        return InterceptionDecision.CONTINUE;
    }

    /**
     * The doExec might be called
     * from {@link java.util.concurrent.ForkJoinTask#invoke()} which might NOT be invoked from ForkJoinPool
     * <p>
     * So, we have to keep the span,
     * initialized in externalPush in the aopContext instead of using TraceContextHolder.detach()
     */
    @Override
    public void after(AopContext aopContext) {
        ITraceSpan span = aopContext.getSpan();
        if (span != null) {
            span.tag(Tags.Thread.NAME, Thread.currentThread().getName())
                .tag(Tags.Thread.ID, Thread.currentThread().getId())
                .finish();
            span.context().finish();

            TraceContextHolder.detach();
        }

        IBithonObject forkJoinTask = (IBithonObject) aopContext.getTarget();
        ForkJoinTaskContext asyncTaskContext = (ForkJoinTaskContext) forkJoinTask.getInjectedObject();
        if (asyncTaskContext.pool != null) {
            // When the pool is null, it means the task is not executed in ForkJoinPool,
            // might be directly called by the invoke in caller's thread

            ThreadPoolMetricRegistry.getInstance().addRunCount(asyncTaskContext.pool,
                                                               aopContext.getExecutionTime() / 1000,
                                                               aopContext.hasException());

            asyncTaskContext.pool = null;
            asyncTaskContext.rootSpan = null;
        }
    }
}
