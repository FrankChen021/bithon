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
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AfterInterceptor;
import org.bithon.agent.observability.tracing.context.ITraceSpan;
import org.bithon.agent.observability.tracing.context.TraceContextFactory;
import org.bithon.agent.plugin.jdk.thread.metrics.ThreadPoolMetricRegistry;
import org.bithon.component.commons.tracing.Tags;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * {@link ForkJoinPool#externalPush(ForkJoinTask)} is an internal method that is called by {@link ForkJoinPool#execute(Runnable)} or {@link ForkJoinPool#submit(Runnable)}.
 *
 * @author frank.chen021@outlook.com
 * @date 2021/2/25 11:15 下午
 */
public class ForkJoinPool$ExternalPush extends AfterInterceptor {

    @Override
    public void after(AopContext aopContext) {
        ForkJoinPool pool = aopContext.getTargetAs();

        //TODO: record task exception of ForkJoinPool
        ThreadPoolMetricRegistry.getInstance().addTotal(pool);

        // Propagate tracing context
        ForkJoinTask<?> task = aopContext.getArgAs(0);
        if (task != null) {
            ITraceSpan span = TraceContextFactory.newSpan("thread-pool");
            if (span != null) {
                aopContext.setSpan(span.method(aopContext.getTargetClass(), aopContext.getMethod())
                                       .tag(Tags.Thread.CLASS, ThreadPoolExecutor.class.getName())
                                       .tag(Tags.Thread.POOL, ((IBithonObject) aopContext.getTarget()).getInjectedObject())
                                       .start());
            }

            // Wrap the users' runnable
            ITraceSpan taskRootSpan = TraceContextFactory.newAsyncSpan("async-task");
            ((IBithonObject) task).setInjectedObject(taskRootSpan);
        }
    }
}
