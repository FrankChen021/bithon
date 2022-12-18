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

package org.bithon.agent.core.metric.domain.http;

import org.bithon.agent.core.metric.collector.MetricRegistry;
import org.bithon.agent.core.metric.collector.MetricRegistryFactory;

import java.util.Arrays;

/**
 * http client
 *
 * @author frankchen
 */
public class HttpOutgoingMetricsRegistry extends MetricRegistry<HttpOutgoingMetrics> {

    public static final String NAME = "http-outgoing-metrics";

    public HttpOutgoingMetricsRegistry() {
        super(NAME,
              Arrays.asList("path", "method", "statusCode"),
              HttpOutgoingMetrics.class,
              HttpOutgoingMetrics::new,
              true);
    }

    public static HttpOutgoingMetricsRegistry get() {
        return MetricRegistryFactory.getOrCreateRegistry(NAME, HttpOutgoingMetricsRegistry::new);
    }

    public HttpOutgoingMetrics addExceptionRequest(String uri,
                                    String method,
                                    long responseTime) {
        String path = uri.split("\\?")[0];
        return getOrCreateMetrics(path, method, "-").addException(responseTime, 1);
    }

    /**
     * @param responseTime in nano-time
     */
    public HttpOutgoingMetrics addRequest(String uri,
                                          String method,
                                          int statusCode,
                                          long responseTime) {
        String path = uri.split("\\?")[0];

        int count4xx = 0, count5xx = 0;
        if (statusCode >= 400) {
            if (statusCode >= 500) {
                count5xx++;
            } else {
                count4xx++;
            }
        }

        HttpOutgoingMetrics metrics = getOrCreateMetrics(path, method, String.valueOf(statusCode));
        metrics.add(responseTime, count4xx, count5xx);
        return metrics;
    }

    public void addBytes(String uri,
                         String method,
                         long requestBytes,
                         long responseBytes) {
        String path = uri.split("\\?")[0];
        getOrCreateMetrics(path, method, "-").addByteSize(requestBytes, responseBytes);
    }
}
