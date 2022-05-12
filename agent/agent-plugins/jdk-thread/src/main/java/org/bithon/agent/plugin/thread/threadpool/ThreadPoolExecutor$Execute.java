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

import org.bithon.agent.bootstrap.aop.AbstractInterceptor;
import org.bithon.agent.bootstrap.aop.AopContext;
import org.bithon.agent.bootstrap.aop.InterceptionDecision;
import org.bithon.agent.core.tracing.context.ITraceContext;
import org.bithon.agent.core.tracing.context.ITraceSpan;
import org.bithon.agent.core.tracing.context.TraceContextHolder;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * {@link ThreadPoolExecutor#execute(Runnable)}
 *
 * @author frank.chen021@outlook.com
 * @date 2022/5/12 8:52 下午
 */
public class ThreadPoolExecutor$Execute extends AbstractInterceptor {

    @Override
    public InterceptionDecision onMethodEnter(AopContext aopContext) {
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
                                                .tag("thread", Thread.currentThread().getName())
                                                .start());

        // change users' runnable
        aopContext.getArgs()[0] = new TracedRunnable(runnable);

        return InterceptionDecision.CONTINUE;
    }

    @Override
    public void onMethodLeave(AopContext aopContext) {
        ITraceSpan span = aopContext.castUserContextAs();
        span.finish();
    }
}
