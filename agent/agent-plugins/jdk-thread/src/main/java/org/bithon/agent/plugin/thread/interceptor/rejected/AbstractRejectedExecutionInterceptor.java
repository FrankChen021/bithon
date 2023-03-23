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

package org.bithon.agent.plugin.thread.interceptor.rejected;

import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.AroundInterceptor;
import org.bithon.agent.instrumentation.aop.interceptor.InterceptionDecision;
import org.bithon.agent.observability.tracing.context.ITraceSpan;
import org.bithon.agent.observability.tracing.context.TraceSpanFactory;
import org.bithon.agent.plugin.thread.utils.ObservedTask;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Consumer;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/25 9:10 下午
 */
public class AbstractRejectedExecutionInterceptor extends AroundInterceptor {

    private final Consumer<ThreadPoolExecutor> onLeave;

    public AbstractRejectedExecutionInterceptor(Consumer<ThreadPoolExecutor> onLeave) {
        this.onLeave = onLeave;
    }

    @Override
    public InterceptionDecision before(AopContext aopContext) {
        Object arg0 = aopContext.getArgs()[0];
        if (arg0 instanceof ObservedTask) {
            // Restore the runnable object for user's application code
            aopContext.getArgs()[0] = ((ObservedTask) arg0).getTask();
        }

        ITraceSpan span = TraceSpanFactory.newSpan("threadPool");
        if (span != null) {
            aopContext.setUserContext(span.method(aopContext.getMethod()).start());
        }
        return InterceptionDecision.CONTINUE;
    }

    @Override
    public void after(AopContext aopContext) {
        ITraceSpan span = (ITraceSpan) aopContext.getUserContext();
        if (span != null) {
            span.tag(aopContext.getException()).finish();
        }

        ThreadPoolExecutor executor = (ThreadPoolExecutor) aopContext.getArgs()[1];
        this.onLeave.accept(executor);
    }
}
