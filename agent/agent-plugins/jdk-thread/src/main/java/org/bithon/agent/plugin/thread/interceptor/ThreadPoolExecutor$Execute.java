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

package org.bithon.agent.plugin.thread.interceptor;

import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.AroundInterceptor;
import org.bithon.agent.instrumentation.aop.interceptor.InterceptionDecision;
import org.bithon.agent.observability.tracing.context.ITraceContext;
import org.bithon.agent.observability.tracing.context.ITraceSpan;
import org.bithon.agent.observability.tracing.context.TraceContextHolder;
import org.bithon.agent.observability.tracing.context.TraceSpanFactory;
import org.bithon.agent.plugin.thread.tracing.TracedRunnable;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * {@link ThreadPoolExecutor#execute(Runnable)}
 *
 * @author frank.chen021@outlook.com
 * @date 2022/5/12 8:52 下午
 */
public class ThreadPoolExecutor$Execute extends AroundInterceptor {

    @Override
    public InterceptionDecision before(AopContext aopContext) {
        ITraceContext currentContext = TraceContextHolder.current();
        if (currentContext == null) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        Runnable runnable = aopContext.getArgAs(0);
        if (runnable == null) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        aopContext.setUserContext(currentContext.currentSpan()
                                                .newChildSpan("threadPool")
                                                .method(aopContext.getMethod())
                                                .start());

        // change users' runnable
        aopContext.getArgs()[0] = new TracedRunnable(runnable,
                                                     TraceSpanFactory.newAsyncSpan("task")
                                                                     .clazz(runnable.getClass().getName())
                                                                     .method("run"));

        return InterceptionDecision.CONTINUE;
    }

    @Override
    public void after(AopContext aopContext) {
        ITraceSpan span = aopContext.getUserContextAs();
        span.finish();
    }
}
