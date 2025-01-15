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

package org.bithon.agent.plugin.apache.zookeeper.metrics;

import org.bithon.agent.observability.metric.collector.MetricRegistry;
import org.bithon.agent.observability.metric.collector.MetricRegistryFactory;

import java.util.Arrays;

/**
 * @author frank.chen021@outlook.com
 * @date 15/1/25 4:32 pm
 */
public class ZKClientMetricRegistry extends MetricRegistry<ZKClientMetrics> {
    public static final String NAME = "zookeeper-client-metrics";

    public ZKClientMetricRegistry() {
        super(NAME,
              Arrays.asList("operation", "status", "server"),
              ZKClientMetrics.class,
              ZKClientMetrics::new,
              true);
    }

    public ZKClientMetrics getOrCreateMetrics(String server,
                                              String operation,
                                              String status) {
        return super.getOrCreateMetrics(operation, status, server);
    }

    private static volatile ZKClientMetricRegistry registryInstance;

    public static ZKClientMetricRegistry getInstance() {
        if (registryInstance == null) {
            synchronized (ZKClientMetricRegistry.class) {
                if (registryInstance == null) {
                    registryInstance = MetricRegistryFactory.getOrCreateRegistry(NAME, ZKClientMetricRegistry::new);
                }
            }
        }
        return registryInstance;
    }
}
