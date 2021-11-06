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

package org.bithon.agent.plugin.undertow.interceptor;

import io.undertow.server.HttpServerExchange;
import org.bithon.agent.bootstrap.aop.AbstractInterceptor;
import org.bithon.agent.bootstrap.aop.AopContext;
import org.bithon.agent.bootstrap.aop.InterceptionDecision;
import org.bithon.agent.core.metric.collector.MetricCollectorManager;
import org.bithon.agent.core.metric.domain.web.HttpIncomingFilter;
import org.bithon.agent.core.metric.domain.web.HttpIncomingMetricsCollector;
import org.bithon.agent.core.tracing.propagation.ITracePropagator;

/**
 * @author frankchen
 */
public class HttpServerExchangeDispatch extends AbstractInterceptor {

    private HttpIncomingFilter requestFilter;
    private HttpIncomingMetricsCollector metricCollector;

    @Override
    public boolean initialize() {
        metricCollector = MetricCollectorManager.getInstance()
                                                .getOrRegister("undertow-web-request-metrics",
                                                               HttpIncomingMetricsCollector.class);

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
            update(exchange, startTime);
            next.proceed();
        });
        return InterceptionDecision.CONTINUE;
    }

    private void update(HttpServerExchange exchange, long startNano) {
        String srcApplication = exchange.getRequestHeaders().getLast(ITracePropagator.BITHON_SRC_APPLICATION);
        String uri = exchange.getRequestPath();
        int httpStatus = exchange.getStatusCode();
        int count4xx = httpStatus >= 400 && httpStatus < 500 ? 1 : 0;
        int count5xx = httpStatus >= 500 ? 1 : 0;
        long requestByteSize = exchange.getRequestContentLength() < 0 ? 0 : exchange.getRequestContentLength();
        long responseByteSize = exchange.getResponseBytesSent();
        long costTime = System.nanoTime() - startNano;

        this.metricCollector.getOrCreateMetric(srcApplication, uri, httpStatus)
                            .updateRequest(costTime, count4xx, count5xx)
                            .updateBytes(requestByteSize, responseByteSize);
    }
}
