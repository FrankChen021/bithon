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

package org.bithon.agent.plugin.spring.scheduling;

import org.bithon.agent.bootstrap.aop.context.AopContext;
import org.bithon.agent.bootstrap.aop.interceptor.AroundInterceptor;
import org.bithon.agent.bootstrap.aop.interceptor.InterceptionDecision;
import org.bithon.agent.observability.tracing.context.ITraceContext;
import org.bithon.agent.observability.tracing.context.ITraceSpan;
import org.bithon.agent.observability.tracing.context.TraceContextHolder;
import org.bithon.component.commons.tracing.SpanKind;
import org.springframework.scheduling.support.ScheduledMethodRunnable;

/**
 * {@link org.springframework.scheduling.support.ScheduledMethodRunnable#run()}
 *
 * @author frankchen
 * @date 13/03/2022 13:45
 */
public class ScheduledMethodRunnable$Run extends AroundInterceptor {

    /**
     * The {@link org.springframework.scheduling.support.ScheduledMethodRunnable} is delegated to {@link org.springframework.scheduling.support.DelegatingErrorHandlingRunnable}.
     * And the interceptor for the latter one is responsible for setting up the tracing context.
     */
    @Override
    public InterceptionDecision before(AopContext aopContext) {
        ITraceContext context = TraceContextHolder.current();
        if (context == null) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        // Don't use runnable.getTarget().getClass().getName()
        // This is because Spring might use cglib to subclass the real user class,
        // and the class that above method returns is the subclass whose name is confusing for end-user
        ScheduledMethodRunnable runnable = aopContext.getTargetAs();
        String targetClass = runnable.getMethod().getDeclaringClass().getName();

        aopContext.setUserContext(context.currentSpan()
                                         .component("spring-scheduler")
                                         .kind(SpanKind.TIMER)
                                         .method(runnable.getMethod())
                                         .tag("uri", "spring-scheduler://" + targetClass)
                                         .start());

        return InterceptionDecision.CONTINUE;
    }

    @Override
    public void after(AopContext aopContext) {
        ITraceSpan span = aopContext.getUserContextAs();

        span.tag(aopContext.getException())
            .tag("status", aopContext.hasException() ? "500" : "200")
            .finish();
        span.context().finish();
    }
}
