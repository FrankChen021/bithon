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

package org.bithon.agent.plugin.apache.kafka.consumer.interceptor;

import org.apache.kafka.common.TopicPartition;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AfterInterceptor;
import org.bithon.agent.observability.metric.collector.MetricRegistryFactory;
import org.bithon.agent.plugin.apache.kafka.KafkaPluginContext;
import org.bithon.agent.plugin.apache.kafka.consumer.metrics.ConsumerMetricRegistry;
import org.bithon.agent.plugin.apache.kafka.consumer.metrics.ConsumerMetrics;

/**
 * {@link org.apache.kafka.clients.consumer.internals.Fetcher.FetchResponseMetricAggregator#record(TopicPartition, int, int)}
 *
 * @author frank.chen021@outlook.com
 * @date 6/7/24 11:14 pm
 */
public class FetchResponseMetricAggregator$Record extends AfterInterceptor {

    private final ConsumerMetricRegistry metricRegistry = MetricRegistryFactory.getOrCreateRegistry("kafka-consumer-metrics", ConsumerMetricRegistry::new);

    @Override
    public void after(AopContext aopContext) throws Exception {
        if (aopContext.hasException()) {
            return;
        }

        // context is injected by FetchResponseMetricAggregator$Ctor
        KafkaPluginContext kafkaPluginContext = aopContext.getInjectedOnTargetAs();
        if (kafkaPluginContext == null) {
            return;
        }

        TopicPartition topicPartition = aopContext.getArgAs(0);
        int bytes = aopContext.getArgAs(1);
        int records = aopContext.getArgAs(2);

        ConsumerMetrics metrics = metricRegistry.getOrCreateMetrics(kafkaPluginContext.broker,
                                                                    kafkaPluginContext.groupId,
                                                                    kafkaPluginContext.clientId,
                                                                    topicPartition.topic(),
                                                                    String.valueOf(topicPartition.partition()));
        metrics.consumedBytes.update(bytes);
        metrics.consumedRecords.update(records);
    }
}
