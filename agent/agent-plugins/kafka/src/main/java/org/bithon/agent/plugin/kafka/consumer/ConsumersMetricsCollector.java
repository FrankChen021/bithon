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
import org.bithon.agent.core.dispatcher.IMessageConverter;
import org.bithon.agent.core.metric.collector.IMetricCollector2;
import org.bithon.agent.core.metric.collector.MetricCollectorManager;
import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.LoggerFactory;
import shaded.com.fasterxml.jackson.core.JsonProcessingException;
import shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
        MetricCollectorManager.getInstance().register("kafka-consumer-metrics", new IMetricCollector2() {
            @Override
            public boolean isEmpty() {
                return false;
            }

            @Override
            public Object collect(IMessageConverter messageConverter,
                                  int interval,
                                  long timestamp) {
                consumers.values()
                         .stream()
                         .map(ManagedKafkaConsumer::collectClientMetrics)
                         .map((metrics) -> {
                             try {
                                 return new ObjectMapper().writeValueAsString(metrics);
                             } catch (JsonProcessingException e) {
                                 return "";
                             }
                         }).forEach(LOG::info);
                return null;
            }
        });

        MetricCollectorManager.getInstance().register("kafka-consumer-topic-metrics", new IMetricCollector2() {
            @Override
            public boolean isEmpty() {
                return false;
            }

            @Override
            public Object collect(IMessageConverter messageConverter,
                                  int interval,
                                  long timestamp) {
                consumers.values()
                         .stream()
                         .map(ManagedKafkaConsumer::collectTopicMetrics)
                         .map((metrics) -> {
                             try {
                                 return new ObjectMapper().writeValueAsString(metrics);
                             } catch (JsonProcessingException e) {
                                 return "";
                             }
                         }).forEach(LOG::info);
                return null;
            }
        });
    }

    public void register(String clientId, KafkaConsumer<?, ?> client, ConsumerConfig config) {
        if (clientId == null) {
            return;
        }

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
