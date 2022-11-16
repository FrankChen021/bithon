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

package org.bithon.agent.plugin.kafka.consumer;

import org.apache.kafka.clients.Metadata;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.bithon.agent.plugin.kafka.consumer.metrics.KafkaConsumerClientMetrics;
import org.bithon.agent.plugin.kafka.consumer.metrics.KafkaConsumerCoordinatorMetrics;
import org.bithon.agent.plugin.kafka.consumer.metrics.KafkaConsumerFetcherMetrics;
import org.bithon.agent.plugin.kafka.consumer.metrics.KafkaConsumerTopicMetrics;
import org.bithon.agent.plugin.kafka.shared.ManagedKafkaClient;
import org.bithon.agent.plugin.kafka.shared.MetricsBuilder;
import org.bithon.component.commons.utils.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ManagedKafkaConsumer extends ManagedKafkaClient {
    private Map<MetricName, Field> fieldsCache;
    private final String groupId;

    public ManagedKafkaConsumer(String clientId, KafkaConsumer<?, ?> client, ConsumerConfig config) {
        super(clientId, client::metrics, (Metadata) ReflectionUtils.getFieldValue(client, "metadata"));
        this.groupId = config.getString(ConsumerConfig.GROUP_ID_CONFIG);
        this.fieldsCache = new HashMap<>();
    }

    @Override
    public void close() {
        super.close();
        this.fieldsCache.clear();
        this.fieldsCache = null;
    }

    public KafkaConsumerClientMetrics collectClientMetrics() {
        List<Map.Entry<MetricName, ? extends Metric>> kafkaMetrics = getKafkaMetrics().entrySet()
                                                                                      .stream()
                                                                                      .filter(e -> "consumer-metrics".equals(e.getKey().group()))
                                                                                      .collect(Collectors.toList());

        KafkaConsumerClientMetrics metrics = MetricsBuilder.toMetrics(kafkaMetrics,
                                                                      KafkaConsumerClientMetrics.class);
        if (metrics != null) {
            metrics.cluster = this.getClusterId();
            metrics.clientId = this.getClientId();
            metrics.groupName = this.groupId;
        }
        return metrics;
    }

    public KafkaConsumerCoordinatorMetrics collectCoordinatorMetrics() {
        List<Map.Entry<MetricName, ? extends Metric>> kafkaMetrics = getKafkaMetrics().entrySet()
                                                                                      .stream()
                                                                                      .filter(e -> "consumer-coordinator-metrics".equals(e.getKey().group()))
                                                                                      .collect(Collectors.toList());

        KafkaConsumerCoordinatorMetrics metrics = MetricsBuilder.toMetrics(kafkaMetrics,
                                                                           KafkaConsumerCoordinatorMetrics.class);
        if (metrics != null) {
        }
        return metrics;
    }

    public KafkaConsumerFetcherMetrics collectFetcherMetrics() {
        List<Map.Entry<MetricName, ? extends Metric>> kafkaMetrics = getKafkaMetrics().entrySet()
                                                                                      .stream()
                                                                                      .filter(e -> "consumer-fetch-manager-metrics".equals(e.getKey().group())
                                                                                              && !e.getKey().tags().containsKey("topic"))
                                                                                      .collect(Collectors.toList());

        KafkaConsumerFetcherMetrics metrics = MetricsBuilder.toMetrics(kafkaMetrics,
                                                                       KafkaConsumerFetcherMetrics.class);
        if (metrics != null) {
            /*
            metrics.cluster = this.getCluster();
            metrics.clientId = this.getClientId();
            metrics.groupName = this.groupId;
             */
        }
        return metrics;
    }

    public List<KafkaConsumerTopicMetrics> collectTopicMetrics() {
        List<Map.Entry<MetricName, ? extends Metric>> kafkaMetrics = getKafkaMetrics().entrySet()
                                                                                      .stream()
                                                                                      .filter(e -> "consumer-fetch-manager-metrics".equals(e.getKey().group())
                                                                                              && e.getKey().tags().containsKey("topic"))
                                                                                      .collect(Collectors.toList());

        Map<String, KafkaConsumerTopicMetrics> metrics = MetricsBuilder.toMetricsGroupBy(kafkaMetrics,
                                                                                         "topic",
                                                                                         KafkaConsumerTopicMetrics.class);
        return metrics.entrySet()
                      .stream()
                      .map(e -> {
                          e.getValue().cluster = this.getClusterId();
                          e.getValue().clientId = this.getClientId();
                          e.getValue().topic = e.getKey();
                          return e.getValue();
                      }).collect(Collectors.toList());
    }
}
