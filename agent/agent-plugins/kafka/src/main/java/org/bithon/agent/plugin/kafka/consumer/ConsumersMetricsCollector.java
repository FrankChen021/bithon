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

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.bithon.agent.core.metric.collector.IMetricCollector3;
import org.bithon.agent.core.metric.collector.MetricCollectorManager;
import org.bithon.agent.plugin.kafka.consumer.metrics.KafkaConsumerClientMetrics;
import org.bithon.agent.plugin.kafka.consumer.metrics.KafkaConsumerTopicMetrics;
import org.bithon.agent.plugin.kafka.shared.MetricsBuilder;
import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author frankchen
 */
public class ConsumersMetricsCollector {
    private static final ILogAdaptor LOG = LoggerFactory.getLogger(ConsumersMetricsCollector.class);

    private static final ConsumersMetricsCollector INSTANCE = new ConsumersMetricsCollector();

    private final Map<String, ManagedKafkaConsumer> consumers = new ConcurrentHashMap<>();

    public static ConsumersMetricsCollector getInstance() {
        return INSTANCE;
    }

    public ConsumersMetricsCollector() {
        MetricCollectorManager.getInstance().register("kafka-consumer-metrics",
                                                      KafkaConsumerClientMetrics.class, new IMetricCollector3<KafkaConsumerClientMetrics>() {
                @Override
                public boolean isEmpty() {
                    return consumers.isEmpty();
                }

                @Override
                public List<KafkaConsumerClientMetrics> collect(int interval, long timestamp) {
                    return consumers.values()
                                    .stream()
                                    .map(this::collect)
                                    .collect(Collectors.toList());
                }

                private KafkaConsumerClientMetrics collect(ManagedKafkaConsumer consumer) {
                    List<Map.Entry<MetricName, ? extends Metric>> kafkaMetrics = consumer.getKafkaMetrics().entrySet()
                                                                                         .stream()
                                                                                         .filter(e -> "consumer-metrics".equals(e.getKey().group()))
                                                                                         .collect(Collectors.toList());

                    KafkaConsumerClientMetrics metrics = MetricsBuilder.toMetricSet(kafkaMetrics,
                                                                                    KafkaConsumerClientMetrics.class);

                    if (metrics != null) {
                        metrics.cluster = consumer.getClusterId();
                        metrics.clientId = consumer.getClientId();
                        metrics.groupId = consumer.getGroupId();
                    }
                    return metrics;
                }
            });

        MetricCollectorManager.getInstance().register("kafka-consumer-topic-metrics",
                                                      KafkaConsumerTopicMetrics.class, new IMetricCollector3<KafkaConsumerTopicMetrics>() {
                @Override
                public boolean isEmpty() {
                    return consumers.isEmpty();
                }

                @Override
                public List<KafkaConsumerTopicMetrics> collect(int interval, long timestamp) {
                    return consumers.values()
                                    .stream()
                                    .map(this::collect)
                                    .flatMap(Collection::stream)
                                    .collect(Collectors.toList());
                }

                private List<KafkaConsumerTopicMetrics> collect(ManagedKafkaConsumer consumer) {
                    List<Map.Entry<MetricName, ? extends Metric>> kafkaMetrics = consumer.getKafkaMetrics().entrySet()
                                                                                         .stream()
                                                                                         .filter(e -> "consumer-fetch-manager-metrics".equals(e.getKey()
                                                                                                                                               .group())
                                                                                                      && e.getKey().tags().containsKey("topic"))
                                                                                         .collect(Collectors.toList());

                    Map<String, KafkaConsumerTopicMetrics> measurement = MetricsBuilder.toMetricsGroupBy(kafkaMetrics,
                                                                                                         "topic",
                                                                                                         KafkaConsumerTopicMetrics.class);
                    return measurement.entrySet()
                                      .stream()
                                      .map(e -> {
                                          e.getValue().topic = e.getKey();
                                          e.getValue().clientId = consumer.getClientId();
                                          e.getValue().cluster = consumer.getClusterId();
                                          e.getValue().groupId = consumer.getGroupId();
                                          return e.getValue();
                                      }).collect(Collectors.toList());
                }
            });
    }

    public void register(String clientId, KafkaConsumer<?, ?> client, ConsumerConfig config) {
        if (clientId == null) {
            return;
        }

        // put consumers together to collect metrics in batch in order to improve performance
        LOG.info("Consumer {} instantiated", clientId);
        consumers.put(clientId, new ManagedKafkaConsumer(clientId, client, config));
    }

    public void unregister(String clientId) {
        if (clientId == null) {
            return;
        }

        ManagedKafkaConsumer consumer = consumers.remove(clientId);
        if (consumer != null) {
            LOG.info("Closing consumer {}", clientId);
            consumer.close();
        }
    }
}
