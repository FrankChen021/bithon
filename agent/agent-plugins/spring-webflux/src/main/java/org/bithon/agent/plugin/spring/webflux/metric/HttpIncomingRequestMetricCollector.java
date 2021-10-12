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

package org.bithon.agent.plugin.spring.webflux.metric;

import org.bithon.agent.core.dispatcher.IMessageConverter;
import org.bithon.agent.core.metric.collector.IntervalMetricCollector;
import org.bithon.agent.core.metric.domain.web.HttpIncomingMetrics;
import org.bithon.agent.core.tracing.propagation.ITracePropagator;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

import java.util.List;

/**
 * @author Frank Chen
 * @date 20201-09-23 00:22
 */
public class HttpIncomingRequestMetricCollector extends IntervalMetricCollector<HttpIncomingMetrics> {

    public void update(HttpServerRequest request, HttpServerResponse response, long responseTime) {
        String uri = request.fullPath();

        String srcApplication = request.requestHeaders().get(ITracePropagator.BITHON_SRC_APPLICATION);

        int httpStatus = response.status().code();
        int count4xx = httpStatus >= 400 && httpStatus < 500 ? 1 : 0;
        int count5xx = httpStatus >= 500 ? 1 : 0;

        HttpIncomingMetrics metric = this.getOrCreateMetric(srcApplication, uri);
        metric.updateRequest(responseTime, count4xx, count5xx);
    }

    public HttpIncomingMetrics getOrCreateMetric(String srcApplication, String uri) {
        return super.getOrCreateMetric(srcApplication == null ? "" : srcApplication, uri);
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
