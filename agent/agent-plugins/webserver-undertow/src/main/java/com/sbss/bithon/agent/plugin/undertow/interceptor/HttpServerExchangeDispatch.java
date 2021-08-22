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

package com.sbss.bithon.agent.plugin.undertow.interceptor;

import com.sbss.bithon.agent.bootstrap.aop.AbstractInterceptor;
import com.sbss.bithon.agent.bootstrap.aop.AopContext;
import com.sbss.bithon.agent.bootstrap.aop.InterceptionDecision;
import com.sbss.bithon.agent.core.metric.collector.MetricCollectorManager;
import com.sbss.bithon.agent.core.metric.domain.web.HttpIncomingFilter;
import com.sbss.bithon.agent.plugin.undertow.metric.WebRequestMetricCollector;
import io.undertow.server.HttpServerExchange;

/**
 * @author frankchen
 */
public class HttpServerExchangeDispatch extends AbstractInterceptor {

    private HttpIncomingFilter requestFilter;
    private WebRequestMetricCollector metricCollector;

    @Override
    public boolean initialize() {
        metricCollector = MetricCollectorManager.getInstance()
                                                .getOrRegister("undertow-web-request-metrics",
                                                               WebRequestMetricCollector.class);

        requestFilter = new HttpIncomingFilter();

        return true;
    }

    @Override
    public InterceptionDecision onMethodEnter(AopContext context) {
        final HttpServerExchange exchange = context.castTargetAs();

        if (this.requestFilter.shouldBeExcluded(exchange.getRequestPath(), exchange.getRequestHeaders().getFirst("User-Agent"))) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        final long startTime = System.nanoTime();
        exchange.addExchangeCompleteListener((listener, next) -> {
            metricCollector.update(exchange, startTime);
            next.proceed();
        });
        return InterceptionDecision.CONTINUE;
    }
}
