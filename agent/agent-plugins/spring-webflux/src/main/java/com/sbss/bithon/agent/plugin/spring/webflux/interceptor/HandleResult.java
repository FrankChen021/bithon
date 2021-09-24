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
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * {@link org.springframework.web.reactive.DispatcherHandler#handleResult(ServerWebExchange, HandlerResult)}
 */
public class HandleResult extends AbstractInterceptor {

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
        ServerWebExchange exchange = (ServerWebExchange) aopContext.getArgs()[0];

        if (requestFilter.shouldBeExcluded(exchange.getRequest().getURI().toString(),
                                           exchange.getRequest().getHeaders().getFirst("User-Agent"))) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        return InterceptionDecision.CONTINUE;
    }

    @Override
    public void onMethodLeave(AopContext aopContext) {

        ServerWebExchange exchange = (ServerWebExchange) aopContext.getArgs()[0];
        if (!(exchange instanceof IBithonObject)) {
            return;
        }

        Mono result = (Mono) aopContext.getReturning();
        aopContext.setReturning(
            result.doFinally(
                type -> metricCollector.update(
                    exchange.getRequest(),
                    exchange.getResponse(),
                    ((WebFluxContext) ((IBithonObject) exchange).getInjectedObject()).getStartTime(),
                    false
                ))
        );
    }
}
