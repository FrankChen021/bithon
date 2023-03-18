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

package org.bithon.agent.plugin.apache.kafka.consumer.metrics;

import org.bithon.agent.observability.metric.collector.MetricRegistry;

import java.util.Arrays;

/**
 * @author frank.chen021@outlook.com
 * @date 2022/12/3 15:39
 */
public class ConsumerMetricRegistry extends MetricRegistry<ConsumerMetrics> {
    public ConsumerMetricRegistry() {
        super("kafka-consumer-metrics",
              Arrays.asList("cluster",
                            "groupId",
                            "clientId",
                            "topic",
                            "partition"),
              ConsumerMetrics.class,
              ConsumerMetrics::new,
              true);
    }

    public ConsumerMetrics getOrCreateMetrics(String cluster,
                                              String groupId,
                                              String clientId,
                                              String topic,
                                              String partition) {
        return super.getOrCreateMetrics(cluster, groupId, clientId, topic, partition);
    }
}
