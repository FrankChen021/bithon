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

import org.bithon.agent.bootstrap.aop.AbstractInterceptor;
import org.bithon.agent.bootstrap.aop.AopContext;
import org.bithon.agent.bootstrap.aop.InterceptionDecision;
import org.bithon.agent.core.tracing.context.ITraceContext;
import org.bithon.agent.core.tracing.context.ITraceSpan;
import org.bithon.agent.core.tracing.context.TraceContextHolder;
import org.bithon.component.commons.tracing.SpanKind;

/**
 * {@link org.springframework.scheduling.support.ScheduledMethodRunnable#run()}
 *
 * @author frankchen
 * @date 13/03/2022 13:45
 */
public class ScheduledMethodRunnable$Run extends AbstractInterceptor {

    /**
     * The {@link org.springframework.scheduling.support.ScheduledMethodRunnable} is delegated to {@link org.springframework.scheduling.support.DelegatingErrorHandlingRunnable}.
     * And the interceptor for the latter one is responsible for setting up the tracing context.
     */
    @Override
    public InterceptionDecision onMethodEnter(AopContext aopContext) {
        ITraceContext context = TraceContextHolder.current();
        if (context == null) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        aopContext.setUserContext(context.currentSpan()
                                         .component("springScheduler")
                                         .kind(SpanKind.TIMER)
                                         .method(aopContext.getMethod())
                                         .tag("uri", "spring-scheduler://" + aopContext.getTargetClass().getName())
                                         .start());

        return InterceptionDecision.CONTINUE;
    }

    @Override
    public void onMethodLeave(AopContext aopContext) {
        ITraceSpan span = aopContext.getUserContextAs();

        span.tag(aopContext.getException())
            .tag("status", aopContext.hasException() ? "500" : "200")
            .finish();
        span.context().finish();
    }
}
