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
import org.bithon.agent.bootstrap.aop.IBithonObject;
import org.bithon.agent.bootstrap.aop.InterceptionDecision;
import org.bithon.agent.core.tracing.context.ITraceContext;
import org.bithon.agent.core.tracing.context.ITraceSpan;
import org.bithon.agent.core.tracing.context.TraceContextHolder;
import org.bithon.agent.plugin.spring.webflux.context.HttpServerContext;
import org.springframework.http.server.reactive.AbstractServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
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
        ServerWebExchange exchange = aopContext.getArgAs(0);

        // ReactorHttpHandlerAdapter#apply creates an object of AbstractServerHttpRequest
        if (!(exchange.getRequest() instanceof AbstractServerHttpRequest)) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        // the request object on exchange is type of HttpServerOperation
        // see ReactorHttpHandlerAdapter#apply
        Object nativeRequest = ((AbstractServerHttpRequest) exchange.getRequest()).getNativeRequest();
        if (!(nativeRequest instanceof IBithonObject)) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        HttpServerContext ctx = (HttpServerContext) ((IBithonObject) nativeRequest).getInjectedObject();
        ITraceContext traceContext = ctx.getTraceContext();
        if (traceContext == null) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        // set to thread local for following calls such as HttpClientFinalizer
        TraceContextHolder.set(traceContext);

        aopContext.setUserContext(traceContext.currentSpan()
                                              .newChildSpan("webflux-routing")
                                              .method(aopContext.getMethod())
                                              .start());
        return InterceptionDecision.CONTINUE;
    }

    @Override
    public void onMethodLeave(AopContext aopContext) {
        TraceContextHolder.remove();

        ITraceSpan span = aopContext.castUserContextAs();
        if (span == null) {
            // in case of exception in the Enter interceptor
            return;
        }

        Mono<Void> originalReturning = aopContext.castReturningAs();
        Mono<Void> replacedReturning = originalReturning.doAfterSuccessOrError((success, error) -> {
            span.tag(error)
                .finish();
        });
        aopContext.setReturning(replacedReturning);
    }
}
