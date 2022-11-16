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

package org.bithon.agent.plugin.kafka.producer;

import org.apache.kafka.clients.Metadata;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.bithon.agent.plugin.kafka.producer.metrics.KafkaProducerClientMetrics;
import org.bithon.agent.plugin.kafka.producer.metrics.KafkaProducerTopicMetrics;
import org.bithon.agent.plugin.kafka.shared.KafkaClientNodeNetworkMetrics;
import org.bithon.agent.plugin.kafka.shared.ManagedKafkaClient;
import org.bithon.agent.plugin.kafka.shared.MetricsBuilder;
import org.bithon.component.commons.utils.ReflectionUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * @author frankchen
 */
public class ManagedKafkaProducer extends ManagedKafkaClient {
    public ManagedKafkaProducer(String clientId, KafkaProducer<?, ?> client) {
        super(clientId,
              client::metrics,
              (Metadata) ReflectionUtils.getFieldValue(client, "metadata"));
    }

    public KafkaProducerClientMetrics collectClientMetrics() {
        List<Map.Entry<MetricName, ? extends Metric>> kafkaMetrics = getKafkaMetrics().entrySet()
                                                                                      .stream()
                                                                                      .filter(e -> "producer-metrics".equals(e.getKey().group()))
                                                                                      .collect(Collectors.toList());
        KafkaProducerClientMetrics metrics = MetricsBuilder.toMetrics(kafkaMetrics,
                                                                      KafkaProducerClientMetrics.class);
        if (metrics != null) {
            metrics.cluster = this.getClusterId();
            metrics.clientId = this.getClientId();
        }
        return metrics;
    }

    public List<KafkaProducerTopicMetrics> collectTopicMetrics() {
        List<Map.Entry<MetricName, ? extends Metric>> kafkaMetrics = getKafkaMetrics().entrySet()
                                                                                      .stream()
                                                                                      .filter(e -> "producer-topic-metrics".equals(e.getKey().group()))
                                                                                      .collect(Collectors.toList());

        Map<String, KafkaProducerTopicMetrics> metrics = MetricsBuilder.toMetricsGroupBy(kafkaMetrics,
                                                                                         "topic",
                                                                                         KafkaProducerTopicMetrics.class);
        return metrics.entrySet().stream().map(e -> {
            e.getValue().cluster = this.getClusterId();
            e.getValue().clientId = this.getClientId();
            e.getValue().topic = e.getKey();
            return e.getValue();
        }).collect(Collectors.toList());
    }

    public List<KafkaClientNodeNetworkMetrics> collectNetworkMetrics() {
        List<Map.Entry<MetricName, ? extends Metric>> kafkaMetrics = getKafkaMetrics().entrySet()
                                                                                      .stream()
                                                                                      .filter(e -> "producer-node-metrics".equals(e.getKey().group()))
                                                                                      .collect(Collectors.toList());

        Map<String, KafkaClientNodeNetworkMetrics> topicEntityMap = MetricsBuilder.toMetricsGroupBy(kafkaMetrics,
                                                                                                    "node-id",
                                                                                                    KafkaClientNodeNetworkMetrics.class);

        /*
         * filter those nodeId=-1
         */
        return topicEntityMap.entrySet().stream().filter(e -> e.getKey().startsWith("node--"))
                             .map(e -> {
                                 e.getValue().cluster = this.getClusterId();
                                 e.getValue().clientId = this.getClientId();
                                 e.getValue().connectionId = e.getKey();
                                 return e.getValue();
                             }).collect(Collectors.toList());
    }
}
