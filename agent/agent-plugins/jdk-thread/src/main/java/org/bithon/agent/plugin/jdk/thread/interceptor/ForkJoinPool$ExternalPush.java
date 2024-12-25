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
import org.bithon.agent.observability.tracing.context.TraceContextFactory;
import org.bithon.component.commons.logging.LoggerFactory;
import org.bithon.component.commons.tracing.Tags;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;

/**
 * {@link ForkJoinPool#externalPush(ForkJoinTask)} is an internal method that is called by {@link ForkJoinPool#execute(Runnable)} or {@link ForkJoinPool#submit(Runnable)}.
 *
 * @author frank.chen021@outlook.com
 * @date 2021/2/25 11:15 下午
 */
public class ForkJoinPool$ExternalPush extends AroundInterceptor {


    @Override
    public InterceptionDecision before(AopContext aopContext) {
        ITraceSpan span = TraceContextFactory.newSpan("thread-pool");
        if (span == null) {
            return InterceptionDecision.CONTINUE;
        }

        IBithonObject forkJoinPool = aopContext.getTargetAs();

        aopContext.setSpan(span.method(aopContext.getTargetClass(), aopContext.getMethod())
                               .tag(Tags.Thread.POOL_CLASS, aopContext.getTargetClass().getName())
                               // The name is injected in the ctor interceptors
                               .tag(Tags.Thread.POOL_NAME, forkJoinPool.getInjectedObject())
                               .tag(Tags.Thread.POOL_PARALLELISM, String.valueOf(((ForkJoinPool) aopContext.getTarget()).getParallelism()))
                               .start());

        return InterceptionDecision.CONTINUE;
    }

    @Override
    public void after(AopContext aopContext) {
        ITraceSpan span = aopContext.getSpan();

        try {
            ForkJoinTask<?> task = aopContext.getArgAs(0);
            if (!(task instanceof IBithonObject)) {
                // If it happens, it might be because this agent is used in a newer version of JDK,
                // which has different implementations of ForkJoinTask
                LoggerFactory.getLogger(ForkJoinPool$ExternalPush.class)
                             .warn("ForkJoinTask is not an instance of IBithonObject: {}", task == null ? "null" : task.getClass().getName());
                return;
            }

            // Injected by ForkJoinTaskAdaptedRunnable$Ctor or ForkJoinTaskRunnableExecutionAction$Ctor ...
            Object taskContext = ((IBithonObject) task).getInjectedObject();
            if (!(taskContext instanceof ForkJoinTaskContext)) {
                LoggerFactory.getLogger(ForkJoinPool$ExternalPush.class)
                             .warn("The inject object is not an instance of ForkJoinTaskContext: {}", taskContext == null ? "null" : taskContext.getClass().getName());
                return;
            }

            // Set up tracing context for this task.
            // The tracing context will be restored in the ForkJoinTask$DoExec
            ForkJoinTaskContext taskContextImpl = (ForkJoinTaskContext) taskContext;
            // Set the pool object for the metrics
            taskContextImpl.pool = aopContext.getTargetAs();
            if (span != null) {
                taskContextImpl.rootSpan = TraceContextFactory.newAsyncSpan("async-task")
                                                              .method(taskContextImpl.className, taskContextImpl.method);
            }
        } finally {
            if (span != null) {
                span.finish();
            }
        }
    }
}
