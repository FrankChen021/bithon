/*
 *    Copyright 2020 bithon.cn
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

package com.sbss.bithon.agent.plugin.spring.webflux.interceptor;

import com.sbss.bithon.agent.bootstrap.aop.AbstractInterceptor;
import com.sbss.bithon.agent.bootstrap.aop.AopContext;
import com.sbss.bithon.agent.bootstrap.aop.IBithonObject;
import com.sbss.bithon.agent.bootstrap.aop.InterceptionDecision;
import com.sbss.bithon.agent.core.metric.collector.MetricCollectorManager;
import com.sbss.bithon.agent.core.metric.domain.web.HttpIncomingFilter;
import com.sbss.bithon.agent.plugin.spring.webflux.WebFluxContext;
import com.sbss.bithon.agent.plugin.spring.webflux.metric.HttpIncomingRequestMetricCollector;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * {@link org.springframework.web.reactive.DispatcherHandler#handle(ServerWebExchange)}
 *
 * @author Frank Chen
 * @date 2021-09-23 00:16
 */
public class Handle extends AbstractInterceptor {

    private HttpIncomingRequestMetricCollector metricCollector;
    private HttpIncomingFilter requestFilter;

    @Override
    public boolean initialize() {
        requestFilter = new HttpIncomingFilter();

        metricCollector = MetricCollectorManager.getInstance()
                                                .getOrRegister("webflux-request-metrics",
                                                               HttpIncomingRequestMetricCollector.class);

        return true;
    }

    @Override
    public InterceptionDecision onMethodEnter(AopContext aopContext) {
        ServerWebExchange webExchange = aopContext.getArgAs(0);
        if (webExchange instanceof IBithonObject) {
            WebFluxContext ctx = new WebFluxContext();
            ctx.setStartTime(System.currentTimeMillis());
            ((IBithonObject) webExchange).setInjectedObject(ctx);
        }

        return InterceptionDecision.CONTINUE;
    }

    @Override
    public void onMethodLeave(AopContext aopContext) throws Exception {
        final long start = System.nanoTime();
        Mono<Void> mono = (Mono<Void>) aopContext.getReturning();
        aopContext.setReturning(mono.doOnSuccessOrError((sucecss, error) -> {
            ServerWebExchange exchange = (ServerWebExchange) aopContext.getArgs()[0];
            if (!(exchange instanceof IBithonObject)) {
                return;
            }

            metricCollector.update(
                exchange.getRequest(),
                exchange.getResponse(),
                System.nanoTime() - start,
                error != null
            );
        }));
    }
}
