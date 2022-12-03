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

package org.bithon.agent.plugin.kafka.consumer.interceptor;

import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.record.Record;
import org.apache.kafka.common.record.RecordBatch;
import org.bithon.agent.bootstrap.aop.AbstractInterceptor;
import org.bithon.agent.bootstrap.aop.AopContext;
import org.bithon.agent.core.metric.collector.MetricRegistryFactory;
import org.bithon.agent.plugin.kafka.consumer.ConsumerContext;
import org.bithon.agent.plugin.kafka.consumer.metrics.ConsumerMetricRegistry;
import org.bithon.agent.plugin.kafka.consumer.metrics.ConsumerMetrics;

import java.util.Arrays;

/**
 * {@link org.apache.kafka.clients.consumer.internals.Fetcher#parseRecord(TopicPartition, RecordBatch, Record)}
 *
 * @author frank.chen021@outlook.com
 * @date 2022/12/3 15:27
 */
public class Fetcher$ParseRecord extends AbstractInterceptor {

    private ConsumerMetricRegistry metricRegistry;

    @Override
    public boolean initialize() {
        metricRegistry = MetricRegistryFactory.getOrCreateRegistry("kafka-consumer-metrics", ConsumerMetricRegistry::new);
        return true;
    }

    @Override
    public void onMethodLeave(AopContext aopContext) {
        TopicPartition topicPartition = aopContext.getArgAs(0);
        Record record = aopContext.getArgAs(2);

        ConsumerContext consumerContext = aopContext.castInjectedOnTargetAs();
        ConsumerMetrics metrics = metricRegistry.getOrCreateMetrics(Arrays.asList(consumerContext.clusterSupplier.get(),
                                                                                  consumerContext.groupId,
                                                                                  consumerContext.clientId,
                                                                                  topicPartition.topic(),
                                                                                  String.valueOf(topicPartition.partition())),
                                                                    ConsumerMetrics::new);
        metrics.consumedBytes.update(record.sizeInBytes());
        metrics.recordCount.update(1);
    }
}
