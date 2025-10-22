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


import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.BeforeInterceptor;
import org.bithon.agent.observability.tracing.context.ITraceContext;
import org.bithon.agent.observability.tracing.context.TraceContextHolder;
import org.bithon.agent.plugin.spring.webflux.context.TracingContextAttributes;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

/**
 * {@link org.springframework.cloud.gateway.handler.FilteringWebHandler.DefaultGatewayFilterChain#DefaultGatewayFilterChain(List)}
 *
 * @author frank.chen021@outlook.com
 * @date 21/10/25 9:45 pm
 */
public class DefaultGatewayFilterChain$Ctor extends BeforeInterceptor {
    @Override
    public void before(AopContext aopContext) {
        List<GatewayFilter> filters = aopContext.getArgAs(0);

        aopContext.getArgs()[0] = filters.stream()
                                         .map((filter) -> {
                                             if (filter instanceof ScopedFilter) {
                                                 return filter;
                                             } else {
                                                 return new ScopedFilter(filter);
                                             }
                                         }).collect(Collectors.toList());
    }

    static class ScopedFilter implements GatewayFilter {
        private final GatewayFilter delegate;

        ScopedFilter(GatewayFilter delegate) {
            this.delegate = delegate;
        }

        @Override
        public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
            // the tracing context is injected in FilteringWebHandler$Handle
            ITraceContext ctx = exchange.getAttribute(TracingContextAttributes.TRACE_CONTEXT);
            if (ctx == null) {
                return delegate.filter(exchange, chain);
            }
            try {
                TraceContextHolder.attach(ctx);
                return delegate.filter(exchange, chain);
            } finally {
                TraceContextHolder.detach();
            }
        }
    }
}
