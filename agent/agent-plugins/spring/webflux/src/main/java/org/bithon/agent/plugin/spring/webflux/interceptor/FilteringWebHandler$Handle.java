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


import org.bithon.agent.instrumentation.aop.IBithonObject;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.InterceptionDecision;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AroundInterceptor;
import org.bithon.agent.observability.tracing.context.ITraceContext;
import org.bithon.agent.observability.tracing.context.TraceMode;
import org.bithon.agent.plugin.spring.webflux.context.HttpServerContext;
import org.bithon.agent.plugin.spring.webflux.context.TracingContextAttributes;
import org.springframework.http.server.reactive.AbstractServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.web.server.ServerWebExchange;

/**
 * This is the entry point of filters in Spring Cloud Gateway
 * {@link org.springframework.cloud.gateway.handler.FilteringWebHandler#handle(ServerWebExchange)}
 *
 * @author frank.chen021@outlook.com
 * @date 7/10/25 4:23 pm
 */
public class FilteringWebHandler$Handle extends AroundInterceptor {
    @Override
    public InterceptionDecision before(AopContext aopContext) {
        ServerWebExchange exchange = aopContext.getArgAs(0);

        ServerHttpRequest request = exchange.getRequest();
        if (request instanceof ServerHttpRequestDecorator) {
            request = ((ServerHttpRequestDecorator) request).getDelegate();
        }

        // ReactorHttpHandlerAdapter#apply creates an object of AbstractServerHttpRequest
        if (!(request instanceof AbstractServerHttpRequest)) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        // the request object on exchange is type of HttpServerOperation
        // see ReactorHttpHandlerAdapter#apply
        Object nativeRequest = ((AbstractServerHttpRequest) request).getNativeRequest();
        if (!(nativeRequest instanceof IBithonObject)) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        HttpServerContext ctx = (HttpServerContext) ((IBithonObject) nativeRequest).getInjectedObject();
        ITraceContext traceContext = ctx.getTraceContext();
        if (traceContext == null || !traceContext.traceMode().equals(TraceMode.TRACING)) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        // Store the trace context in the exchange so that each filter can retrieve it
        // This is necessary because filters may execute on different threads in the reactive pipeline
        exchange.getAttributes().put(TracingContextAttributes.TRACE_CONTEXT, traceContext);

        // Set the context for the current thread
        //TraceContextHolder.attach(traceContext);
        return InterceptionDecision.CONTINUE;
    }

    @Override
    public void after(AopContext aopContext) {
        // Detach from current thread since the filter chain will execute on different threads
        // Each filter will attach/detach the context as needed
        //TraceContextHolder.detach();
    }
}
