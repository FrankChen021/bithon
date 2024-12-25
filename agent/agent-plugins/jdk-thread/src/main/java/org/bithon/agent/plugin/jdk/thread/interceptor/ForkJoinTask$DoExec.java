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
import org.bithon.agent.observability.tracing.context.ITraceContext;
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
            asyncTaskContext.rootSpan.start();
        }

        return InterceptionDecision.CONTINUE;
    }

    @Override
    public void after(AopContext aopContext) {
        ITraceContext ctx = TraceContextHolder.detach();
        if (ctx != null) {
            ctx.currentSpan()
               .tag(Tags.Thread.NAME, Thread.currentThread().getName())
               .tag(Tags.Thread.ID, Thread.currentThread().getId())
               .finish();
            ctx.finish();
        }

        IBithonObject forkJoinTask = (IBithonObject) aopContext.getTarget();
        ForkJoinTaskContext asyncTaskContext = (ForkJoinTaskContext) forkJoinTask.getInjectedObject();
        ThreadPoolMetricRegistry.getInstance().addRunCount(asyncTaskContext.pool,
                                                           aopContext.getExecutionTime() / 1000,
                                                           aopContext.hasException());

        asyncTaskContext.pool = null;
        asyncTaskContext.rootSpan = null;
    }
}
