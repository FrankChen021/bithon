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

package org.bithon.agent.core.metric.domain.web;

import org.bithon.agent.core.metric.collector.MetricRegistry;
import org.bithon.agent.core.metric.collector.MetricRegistryFactory;

import java.util.Arrays;


/**
 * @author frankchen
 */
public class HttpIncomingMetricsRegistry extends MetricRegistry<HttpIncomingMetrics> {

    public HttpIncomingMetricsRegistry() {
        super("http-incoming-metrics",
              Arrays.asList("srcApplication", "method", "uri", "statusCode"),
              HttpIncomingMetrics.class,
              HttpIncomingMetrics::new,
              true);
    }

    public static HttpIncomingMetricsRegistry get() {
        return MetricRegistryFactory.getOrCreateRegistry("http-incoming-metrics", HttpIncomingMetricsRegistry::new);
    }

    public HttpIncomingMetrics getOrCreateMetrics(String srcApplication, String method, String uri, int statusCode) {
        return super.getOrCreateMetrics(srcApplication == null ? "" : srcApplication, method, uri, String.valueOf(statusCode));
    }
}
