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
import org.bithon.agent.core.dispatcher.IMessageConverter;
import org.bithon.agent.core.metric.collector.IMetricCollector2;
import org.bithon.agent.core.metric.collector.MetricCollectorManager;
import org.bithon.agent.plugin.kafka.consumer.metrics.KafkaConsumerClientMetrics;
import org.bithon.agent.plugin.kafka.consumer.metrics.KafkaConsumerTopicMetrics;
import org.bithon.agent.plugin.kafka.shared.MetricsBuilder;
import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.LoggerFactory;
import shaded.com.fasterxml.jackson.core.JsonProcessingException;
import shaded.com.fasterxml.jackson.databind.ObjectMapper;

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
        MetricCollectorManager.getInstance().register("kafka-consumer-metrics", /* Add new paramter Schema here */ new IMetricCollector2() {
            @Override
            public boolean isEmpty() {
                return consumers.isEmpty();
            }

            @Override
            public Object collect(IMessageConverter messageConverter,
                                  int interval,
                                  long timestamp) {
                consumers.values()
                         .stream()
                         .map(this::collect)
                         .map((metrics) -> {
                             try {
                                 return new ObjectMapper().writeValueAsString(metrics);
                             } catch (JsonProcessingException e) {
                                 return "";
                             }
                         }).forEach(LOG::info);
                return null;
            }

            /**
             * TODO: Modification to returning
             * @return {@link java.util.List<org.bithon.agent.core.metric.collector.IMeasurement>}
             */
            private KafkaConsumerClientMetrics collect(ManagedKafkaConsumer consumer) {
                List<Map.Entry<MetricName, ? extends Metric>> kafkaMetrics = consumer.getKafkaMetrics().entrySet()
                                                                                     .stream()
                                                                                     .filter(e -> "consumer-metrics".equals(e.getKey().group()))
                                                                                     .collect(Collectors.toList());

                KafkaConsumerClientMetrics.Measurement measurement = MetricsBuilder.toMetrics(kafkaMetrics,
                                                                                              KafkaConsumerClientMetrics.Measurement.class);

                KafkaConsumerClientMetrics metrics = null;
                if (measurement != null) {
                    metrics = new KafkaConsumerClientMetrics();
                    metrics.cluster = consumer.getClusterId();
                    metrics.clientId = consumer.getClientId();
                    metrics.groupName = consumer.getGroupId();
                    metrics.measurement = measurement;
                }
                return metrics;
            }
        });

        MetricCollectorManager.getInstance().register("kafka-consumer-topic-metrics", new IMetricCollector2() {
            @Override
            public boolean isEmpty() {
                return consumers.isEmpty();
            }

            @Override
            public Object collect(IMessageConverter messageConverter,
                                  int interval,
                                  long timestamp) {
                consumers.values()
                         .stream()
                         .map(this::collect)
                         .map((metrics) -> {
                             try {
                                 return new ObjectMapper().writeValueAsString(metrics);
                             } catch (JsonProcessingException e) {
                                 return "";
                             }
                         }).forEach(LOG::info);
                return null;
            }

            private List<KafkaConsumerTopicMetrics> collect(ManagedKafkaConsumer consumer) {
                List<Map.Entry<MetricName, ? extends Metric>> kafkaMetrics = consumer.getKafkaMetrics().entrySet()
                                                                                     .stream()
                                                                                     .filter(e -> "consumer-fetch-manager-metrics".equals(e.getKey().group())
                                                                                                  && e.getKey().tags().containsKey("topic"))
                                                                                     .collect(Collectors.toList());

                Map<String, KafkaConsumerTopicMetrics.Measurement> measurement = MetricsBuilder.toMetricsGroupBy(kafkaMetrics,
                                                                                                                 "topic",
                                                                                                                 KafkaConsumerTopicMetrics.Measurement.class);
                return measurement.entrySet()
                                  .stream()
                                  .map(e -> {
                                      KafkaConsumerTopicMetrics metrics = new KafkaConsumerTopicMetrics();
                                      metrics.topic = e.getKey();
                                      metrics.clientId = consumer.getClientId();
                                      metrics.cluster = consumer.getClusterId();
                                      metrics.groupId = consumer.getGroupId();
                                      metrics.measurement = e.getValue();
                                      return metrics;
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
