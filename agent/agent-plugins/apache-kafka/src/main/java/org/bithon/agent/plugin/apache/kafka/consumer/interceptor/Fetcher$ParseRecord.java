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
import org.apache.kafka.common.record.Record;
import org.apache.kafka.common.record.RecordBatch;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.AfterInterceptor;
import org.bithon.agent.observability.metric.collector.MetricRegistryFactory;
import org.bithon.agent.plugin.apache.kafka.KafkaPluginContext;
import org.bithon.agent.plugin.apache.kafka.consumer.metrics.ConsumerMetricRegistry;
import org.bithon.agent.plugin.apache.kafka.consumer.metrics.ConsumerMetrics;

/**
 * {@link org.apache.kafka.clients.consumer.internals.Fetcher#parseRecord(TopicPartition, RecordBatch, Record)}
 *
 * @author frank.chen021@outlook.com
 * @date 2022/12/3 15:27
 */
public class Fetcher$ParseRecord extends AfterInterceptor {

    private final ConsumerMetricRegistry metricRegistry = MetricRegistryFactory.getOrCreateRegistry("kafka-consumer-metrics", ConsumerMetricRegistry::new);

    @Override
    public void after(AopContext aopContext) {
        TopicPartition topicPartition = aopContext.getArgAs(0);
        Record record = aopContext.getArgAs(2);

        KafkaPluginContext kafkaPluginContext = aopContext.getInjectedOnTargetAs();
        ConsumerMetrics metrics = metricRegistry.getOrCreateMetrics(kafkaPluginContext.clusterSupplier.get(),
                                                                    kafkaPluginContext.groupId,
                                                                    kafkaPluginContext.clientId,
                                                                    topicPartition.topic(),
                                                                    String.valueOf(topicPartition.partition()));
        metrics.consumedBytes.update(record.sizeInBytes());
        metrics.consumedRecords.update(1);
    }
}
