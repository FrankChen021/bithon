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

package org.bithon.agent.plugin.spring.webflux.gateway.aop;

import org.bithon.agent.bootstrap.aop.IBithonObject;
import org.bithon.agent.bootstrap.aop.InterceptionDecision;
import org.bithon.agent.bootstrap.aop.advice.IAdviceInterceptor;
import org.bithon.agent.core.tracing.context.ITraceContext;
import org.bithon.agent.core.tracing.context.ITraceSpan;
import org.bithon.agent.core.tracing.context.TraceContextHolder;
import org.bithon.agent.plugin.spring.webflux.context.HttpServerContext;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.server.reactive.AbstractServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;

/**
 * This interceptor hooks the target {@link org.springframework.cloud.gateway.filter.GlobalFilter#filter(ServerWebExchange, GatewayFilterChain)}
 * <p>
 * the interception stops when the returned value is terminated.
 * <p>
 * See {@link org.springframework.cloud.gateway.filter.NettyRoutingFilter} for an example
 *
 * @author Frank Chen
 * @date 29/11/21 4:39 pm
 */
public class AroundGatewayFilterInterceptor implements IAdviceInterceptor {


    @Override
    public Object onMethodEnter(Method method, Object target, Object[] args) {
        ServerWebExchange exchange = (ServerWebExchange) args[0];

        // some filters may turn the raw request to a decorated request
        ServerHttpRequest httpRequest = exchange.getRequest();
        if (httpRequest instanceof ServerHttpRequestDecorator) {
            httpRequest = ((ServerHttpRequestDecorator) httpRequest).getDelegate();
        }
        // ReactorHttpHandlerAdapter#apply creates an object of AbstractServerHttpRequest
        if (!(httpRequest instanceof AbstractServerHttpRequest)) {
            return null;
        }

        // the request object on exchange is type of HttpServerOperation
        // see ReactorHttpHandlerAdapter#apply
        Object nativeRequest = ((AbstractServerHttpRequest) httpRequest).getNativeRequest();
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

        return traceContext.currentSpan()
                           .newChildSpan("filter")
                           .method(method)
                           .start();
    }

    @Override
    public Object onMethodExit(Method method, Object target, Object[] args, Object returning, Throwable exception, Object context) {
        TraceContextHolder.remove();

        ITraceSpan span = (ITraceSpan) context;
        if (span == null) {
            // in case of exception in the Enter interceptor
            return returning;
        }

        //noinspection unchecked
        Mono<Void> originalReturning = (Mono<Void>) returning;

        // replace the returning
        //noinspection deprecation,ReactiveStreamsUnusedPublisher
        return originalReturning.doAfterSuccessOrError((success, error) -> span.tag(error).finish());
    }
}
