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
import org.bithon.agent.core.plugin.PluginConfigurationManager;
import org.bithon.agent.core.tracing.context.ITraceContext;
import org.bithon.agent.core.tracing.context.ITraceSpan;
import org.bithon.agent.core.tracing.context.TraceContextHolder;
import org.bithon.agent.plugin.spring.webflux.SpringWebFluxPlugin;
import org.bithon.agent.plugin.spring.webflux.config.GatewayFilterConfigs;
import org.bithon.agent.plugin.spring.webflux.context.HttpServerContext;
import org.springframework.http.server.reactive.AbstractServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * {@link org.springframework.cloud.gateway.filter.NettyRoutingFilter}
 * <p>
 * This interceptor is not enabled because the callback is executed before
 *
 * @author Frank Chen
 * @date 29/11/21 4:39 pm
 */
public class AroundGatewayFilter$Filter extends AbstractInterceptor {

    private final GatewayFilterConfigs configs;

    public AroundGatewayFilter$Filter() {
        configs = PluginConfigurationManager.load(SpringWebFluxPlugin.class).getConfig(GatewayFilterConfigs.class);
    }

    @Override
    public InterceptionDecision onMethodEnter(AopContext aopContext) {
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
        if (traceContext == null) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        // Set the context for each filter
        //
        // Once upon time we did it in the ChannelOperations#onInboundComplete which is called by HttpServerOperations
        // that's the entry point for the incoming HTTP requests which is schedule on the HTTP nio threads
        // But for a repeated/retried request, it's scheduled on parallel scheduler threads, so any code that retrieves the context fails
        TraceContextHolder.set(traceContext);

        aopContext.setUserContext(traceContext.currentSpan()
                                              .newChildSpan("filter")
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

        final ServerWebExchange exchange = aopContext.getArgAs(0);
        FilterUtils.extractAttributesAsTraceTags(exchange, this.configs, aopContext.getTargetClass(), span);

        if (aopContext.hasException()) {
            // this exception might be thrown from this filter or from the chains of the filter.
            // For the 1st case, the span is not closed, so we have to finish it
            // For the 2nd case, the span is closed before entering the filter chain. It's safe to call the finish method once more
            span.tag(aopContext.getException()).finish();
        } else {
            Mono<Void> originalReturning = aopContext.castReturningAs();
            Mono<Void> replacedReturning = originalReturning.doOnError((exception) -> span.tag(exception).finish())
                                                            .doOnSuccess((s) -> span.finish());
            aopContext.setReturning(replacedReturning);
        }
    }
}
