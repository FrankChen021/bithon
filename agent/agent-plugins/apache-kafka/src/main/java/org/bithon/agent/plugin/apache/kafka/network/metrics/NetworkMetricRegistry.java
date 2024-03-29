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

package org.bithon.agent.plugin.apache.kafka.network.metrics;

import org.bithon.agent.observability.metric.collector.MetricRegistry;

import java.util.Arrays;

/**
 * @author frank.chen021@outlook.com
 * @date 2022/12/3 16:24
 */
public class NetworkMetricRegistry extends MetricRegistry<NetworkMetrics> {
    public NetworkMetricRegistry() {
        super("kafka-network-metrics",
              Arrays.asList("type", "cluster", "nodeId", "groupId", "clientId", "exception"),
              NetworkMetrics.class,
              NetworkMetrics::new,
              true);
    }

    public NetworkMetrics getOrCreateMetrics(String type,
                                             String cluster,
                                             String nodeId,
                                             String groupId,
                                             String clientId,
                                             String exception) {
        return super.getOrCreateMetrics(type, cluster, nodeId, groupId, clientId, exception);
    }
}
