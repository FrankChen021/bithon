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

import org.bithon.agent.configuration.ConfigurationManager;
import org.bithon.agent.instrumentation.aop.IBithonObject;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.InterceptionDecision;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AroundInterceptor;
import org.bithon.agent.observability.tracing.context.ITraceContext;
import org.bithon.agent.observability.tracing.context.ITraceSpan;
import org.bithon.agent.observability.tracing.context.TraceMode;
import org.bithon.agent.plugin.spring.webflux.config.GatewayFilterConfigs;
import org.bithon.agent.plugin.spring.webflux.context.HttpServerContext;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.server.reactive.AbstractServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.web.server.ServerWebExchange;

/**
 * @author frank.chen021@outlook.com
 * @date 28/12/21 12:08 PM
 */
public class BeforeGatewayFilter$Filter extends AroundInterceptor {

    private final GatewayFilterConfigs configs;

    public BeforeGatewayFilter$Filter() {
        configs = ConfigurationManager.getInstance().getConfig(GatewayFilterConfigs.class);
    }

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

        ITraceSpan span = traceContext.currentSpan()
                                      .newChildSpan("filter")
                                      .method(aopContext.getTargetClass(), aopContext.getMethod())
                                      .start();

        // replace the input argument
        GatewayFilterChain delegate = aopContext.getArgAs(1);
        aopContext.getArgs()[1] = (GatewayFilterChain) exchange1 -> {
            span.finish();
            return delegate.filter(exchange1);
        };

        aopContext.setSpan(span);
        return InterceptionDecision.CONTINUE;
    }

    @Override
    public void after(AopContext aopContext) {
        final ServerWebExchange exchange = aopContext.getArgAs(0);
        ITraceSpan span = aopContext.getSpan();
        if (span == null) {
            return;
        }

        FilterUtils.extractAttributesAsTraceTags(exchange, this.configs, aopContext.getTargetClass(), span);

        if (aopContext.hasException()) {
            // This exception might be thrown from this filter or from the chains of the filter.
            // For the 1st case, the span is not closed, so we have to finish it
            // For the 2nd case, the span is closed before entering the filter chain. It's safe to call the finish method once more
            span.tag(aopContext.getException()).finish();
        }
    }
}
