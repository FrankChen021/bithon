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

package org.bithon.agent.plugin.undertow.metric;

import io.undertow.server.HttpServerExchange;
import org.bithon.agent.core.dispatcher.IMessageConverter;
import org.bithon.agent.core.metric.collector.IntervalMetricCollector;
import org.bithon.agent.core.metric.domain.web.HttpIncomingMetrics;
import org.bithon.agent.core.tracing.propagation.ITracePropagator;

import java.util.List;

/**
 * @author frankchen
 */
public class WebRequestMetricCollector extends IntervalMetricCollector<HttpIncomingMetrics> {

    public void update(HttpServerExchange exchange, long startNano) {
        String srcApplication = exchange.getRequestHeaders().getLast(ITracePropagator.BITHON_SRC_APPLICATION);
        String uri = exchange.getRequestPath();
        int httpStatus = exchange.getStatusCode();
        int count4xx = httpStatus >= 400 && httpStatus < 500 ? 1 : 0;
        int count5xx = httpStatus >= 500 ? 1 : 0;
        long requestByteSize = exchange.getRequestContentLength() < 0 ? 0 : exchange.getRequestContentLength();
        long responseByteSize = exchange.getResponseBytesSent();
        long costTime = System.nanoTime() - startNano;

        HttpIncomingMetrics metric = getOrCreateMetric(srcApplication == null ? "" : srcApplication, uri);
        metric.updateRequest(costTime, count4xx, count5xx);
        metric.updateBytes(requestByteSize, responseByteSize);
    }

    @Override
    protected HttpIncomingMetrics newMetrics() {
        return new HttpIncomingMetrics();
    }

    @Override
    protected Object toMessage(IMessageConverter messageConverter,
                               int interval,
                               long timestamp,
                               List<String> dimensions,
                               HttpIncomingMetrics metric) {
        return messageConverter.from(timestamp, interval, dimensions, metric);
    }
}
