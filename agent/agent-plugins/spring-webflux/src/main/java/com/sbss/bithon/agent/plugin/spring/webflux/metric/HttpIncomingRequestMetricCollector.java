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

package com.sbss.bithon.agent.plugin.spring.webflux.metric;

import com.sbss.bithon.agent.core.dispatcher.IMessageConverter;
import com.sbss.bithon.agent.core.metric.collector.IntervalMetricCollector;
import com.sbss.bithon.agent.core.metric.domain.web.HttpIncomingMetrics;
import com.sbss.bithon.agent.core.tracing.propagation.ITracePropagator;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;

import java.util.List;

/**
 * @author Frank Chen
 * @date {20201-09-23} {00:22}
 */
public class HttpIncomingRequestMetricCollector extends IntervalMetricCollector<HttpIncomingMetrics> {

    public void update(ServerHttpRequest request, ServerHttpResponse response, long responseTime) {
        String uri = request.getURI().getPath();
        if (uri == null) {
            return;
        }

        String srcApplication = request.getHeaders().getFirst(ITracePropagator.BITHON_SRC_APPLICATION);

        int httpStatus = response.getStatusCode().value();
        int count4xx = httpStatus >= 400 && httpStatus < 500 ? 1 : 0;
        int count5xx = httpStatus >= 500 ? 1 : 0;
        long requestByteSize = request.getHeaders().getContentLength();
        long responseByteSize = response.getHeaders().getContentLength();

        HttpIncomingMetrics metric = getOrCreateMetric(srcApplication == null ? "" : srcApplication, uri);
        metric.updateRequest(responseTime, count4xx, count5xx);
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
