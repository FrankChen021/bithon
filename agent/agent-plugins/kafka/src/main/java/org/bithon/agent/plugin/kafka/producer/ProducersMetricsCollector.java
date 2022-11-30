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

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.bithon.agent.core.metric.collector.IMetricCollector3;
import org.bithon.agent.core.metric.collector.MetricCollectorManager;
import org.bithon.agent.plugin.kafka.producer.metrics.KafkaProducerClientMetrics;
import org.bithon.agent.plugin.kafka.producer.metrics.KafkaProducerTopicMetrics;
import org.bithon.agent.plugin.kafka.shared.KafkaClientNodeNetworkMetrics;
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
public class ProducersMetricsCollector {
    private static final ILogAdaptor LOG = LoggerFactory.getLogger(ProducersMetricsCollector.class);
    private static final ProducersMetricsCollector INSTANCE = new ProducersMetricsCollector();

    /**
     * key: client-id
     */
    private final Map<String, ManagedKafkaProducer> producers = new ConcurrentHashMap<>();

    public static ProducersMetricsCollector getInstance() {
        return INSTANCE;
    }

    public ProducersMetricsCollector() {
        MetricCollectorManager.getInstance().register("kafka-producer-metrics",
                                                      KafkaProducerClientMetrics.class, new IMetricCollector3<KafkaProducerClientMetrics>() {
                @Override
                public boolean isEmpty() {
                    return producers.isEmpty();
                }

                @Override
                public List<KafkaProducerClientMetrics> collect(int interval, long timestamp) {
                    return producers.values()
                                    .stream()
                                    .map(this::collect)
                                    .collect(Collectors.toList());
                }

                private KafkaProducerClientMetrics collect(ManagedKafkaProducer producer) {
                    List<Map.Entry<MetricName, ? extends Metric>> kafkaMetrics = producer.getKafkaMetrics()
                                                                                         .entrySet()
                                                                                         .stream()
                                                                                         .filter(e -> "producer-metrics".equals(e.getKey().group()))
                                                                                         .collect(Collectors.toList());
                    KafkaProducerClientMetrics metrics = MetricsBuilder.toMetricSet(kafkaMetrics,
                                                                                    KafkaProducerClientMetrics.class);

                    if (metrics != null) {
                        metrics.clientId = producer.getClientId();
                        metrics.cluster = producer.getClusterId();
                    }
                    return metrics;
                }
            });

        MetricCollectorManager.getInstance().register("kafka-producer-topic-metrics",
                                                      KafkaProducerTopicMetrics.class, new IMetricCollector3<KafkaProducerTopicMetrics>() {
                @Override
                public boolean isEmpty() {
                    return producers.isEmpty();
                }

                @Override
                public List<KafkaProducerTopicMetrics> collect(int interval, long timestamp) {
                    return producers.values()
                                    .stream()
                                    .map(this::collect)
                                    .flatMap(Collection::stream)
                                    .collect(Collectors.toList());
                }

                private List<KafkaProducerTopicMetrics> collect(ManagedKafkaProducer producer) {
                    List<Map.Entry<MetricName, ? extends Metric>> kafkaMetrics = producer.getKafkaMetrics().entrySet()
                                                                                         .stream()
                                                                                         .filter(e -> "producer-topic-metrics".equals(
                                                                                             e.getKey().group()))
                                                                                         .collect(Collectors.toList());

                    Map<String, KafkaProducerTopicMetrics> measurement = MetricsBuilder.toMetricsGroupBy(kafkaMetrics,
                                                                                                         "topic",
                                                                                                         KafkaProducerTopicMetrics.class);
                    return measurement.entrySet().stream().map(e -> {
                        e.getValue().topic = e.getKey();
                        e.getValue().cluster = producer.getClusterId();
                        e.getValue().clientId = producer.getClientId();
                        return e.getValue();
                    }).collect(Collectors.toList());
                }
            });

        MetricCollectorManager.getInstance().register("kafka-producer-network-metrics",
                                                      KafkaClientNodeNetworkMetrics.class, new IMetricCollector3<KafkaClientNodeNetworkMetrics>() {
                @Override
                public boolean isEmpty() {
                    return producers.isEmpty();
                }

                @Override
                public List<KafkaClientNodeNetworkMetrics> collect(int interval, long timestamp) {
                    return producers.values()
                                    .stream()
                                    .map(this::collect)
                                    .flatMap(Collection::stream)
                                    .collect(Collectors.toList());
                }

                public List<KafkaClientNodeNetworkMetrics> collect(ManagedKafkaProducer producer) {
                    List<Map.Entry<MetricName, ? extends Metric>> kafkaMetrics = producer.getKafkaMetrics().entrySet()
                                                                                         .stream()
                                                                                         .filter(e -> "producer-node-metrics".equals(e.getKey().group()))
                                                                                         .collect(Collectors.toList());

                    Map<String, KafkaClientNodeNetworkMetrics> measurements = MetricsBuilder.toMetricsGroupBy(kafkaMetrics,
                                                                                                              "node-id",
                                                                                                              KafkaClientNodeNetworkMetrics.class);

                    /*
                     * filter those nodeId=-1
                     */
                    return measurements.entrySet()
                                       .stream()
                                       .filter(e -> e.getKey().startsWith("node--"))
                                       .map(e -> {
                                           e.getValue().cluster = producer.getClusterId();
                                           e.getValue().clientId = producer.getClientId();
                                           e.getValue().connectionId = e.getKey();
                                           return e.getValue();
                                       }).collect(Collectors.toList());
                }
            });
    }

    public void register(String clientId, KafkaProducer<?, ?> producer) {
        if (clientId == null) {
            return;
        }

        LOG.info("Producer {} instantiated", clientId);
        producers.put(clientId,
                      new ManagedKafkaProducer(clientId, producer));
    }

    public void unregister(String clientId) {
        if (clientId == null) {
            return;
        }

        ManagedKafkaProducer producerInfo = producers.remove(clientId);
        if (producerInfo != null) {
            LOG.info("Closing producer {}", clientId);
            producerInfo.close();
        }
    }
}
