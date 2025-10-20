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
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.InterceptionDecision;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AroundInterceptor;
import org.bithon.agent.observability.tracing.context.ITraceContext;
import org.bithon.agent.observability.tracing.context.ITraceSpan;
import org.bithon.agent.observability.tracing.context.TraceContextHolder;
import org.bithon.agent.observability.tracing.context.TraceMode;
import org.bithon.agent.plugin.spring.webflux.config.GatewayFilterConfigs;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * {@link org.springframework.cloud.gateway.filter.NettyRoutingFilter}
 * <p>
 * This interceptor is not enabled because the callback is executed before
 *
 * @author frank.chen021@outlook.com
 * @date 29/11/21 4:39 pm
 */
public class AroundGatewayFilter$Filter extends AroundInterceptor {

    private final GatewayFilterConfigs configs;

    public AroundGatewayFilter$Filter() {
        configs = ConfigurationManager.getInstance().getConfig(GatewayFilterConfigs.class);
    }

    @Override
    public InterceptionDecision before(AopContext aopContext) {
        ITraceContext ctx = TraceContextHolder.current();
        if (ctx == null || ctx.traceMode() == TraceMode.LOGGING) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        aopContext.setSpan(ctx.currentSpan()
                              .newChildSpan("filter")
                              .method(aopContext.getTargetClass(), aopContext.getMethod())
                              .start());

        return InterceptionDecision.CONTINUE;
    }

    @Override
    public void after(AopContext aopContext) {
        ITraceSpan span = aopContext.getSpan();
        if (span == null) {
            // in case of exception in the Enter interceptor
            return;
        }

        final ServerWebExchange exchange = aopContext.getArgAs(0);
        FilterUtils.extractAttributesAsTraceTags(exchange, this.configs, aopContext.getTargetClass(), span);

        if (aopContext.hasException()) {
            // This exception might be thrown from this filter or from the chains of the filter.
            // For the 1st case, the span is not closed, so we have to finish it
            // For the 2nd case, the span is closed before entering the filter chain. It's safe to call the finish method once more
            span.tag(aopContext.getException()).finish();
        } else {
            Mono<Void> originalReturning = aopContext.getReturningAs();
            Mono<Void> replacedReturning = originalReturning.doOnError((throwable -> {
                                                                // NOTE: DO NOT Replace the call by lambda which is suggested by the IDE,
                                                                // because it's hard to debug the statement
                                                                span.tag(throwable).finish();
                                                            }))
                                                            .doOnSuccess((s) -> {
                                                                // NOTE: DO NOT Replace the call by lambda which is suggested by the IDE,
                                                                // because it's hard to debug the statement
                                                                span.finish();
                                                            });
            aopContext.setReturning(replacedReturning);
        }
    }
}
