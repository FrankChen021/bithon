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

package org.bithon.agent.plugin.spring.webflux.interceptor;

import org.bithon.agent.bootstrap.aop.AbstractInterceptor;
import org.bithon.agent.bootstrap.aop.AopContext;
import org.bithon.agent.bootstrap.aop.InterceptionDecision;
import org.bithon.agent.core.tracing.context.ITraceContext;
import org.bithon.agent.core.tracing.context.ITraceSpan;
import org.bithon.agent.core.tracing.context.SpanKind;
import org.bithon.agent.core.tracing.context.TraceContextHolder;
import reactor.core.publisher.Mono;

/**
 * {@linkto org.springframework.cloud.gateway.filter.NettyRoutingFilter}
 * <p>
 * This interceptor is not enabled because the callback is executed before
 *
 * @author Frank Chen
 * @date 29/11/21 4:39 pm
 */
public class NettyRoutingFilter$Filter extends AbstractInterceptor {

    @Override
    public InterceptionDecision onMethodEnter(AopContext aopContext) {
        ITraceContext traceContext = TraceContextHolder.current();
        if (traceContext == null) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        aopContext.setUserContext(traceContext.currentSpan()
                                              .newChildSpan("webflux-routing")
                                              .kind(SpanKind.CLIENT)
                                              .method(aopContext.getMethod())
                                              .start());
        return InterceptionDecision.CONTINUE;
    }

    @Override
    public void onMethodLeave(AopContext aopContext) {
        ITraceSpan span = aopContext.castUserContextAs();
        Mono<Void> originalReturning = aopContext.castReturningAs();
        Mono<Void> replacedReturning = originalReturning.doAfterSuccessOrError((success, error) -> {
            span.tag(error)
                .finish();
        });
        aopContext.setReturning(replacedReturning);
    }
}
