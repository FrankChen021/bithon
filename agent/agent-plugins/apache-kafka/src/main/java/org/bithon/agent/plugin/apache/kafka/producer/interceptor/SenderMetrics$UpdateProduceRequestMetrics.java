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

package org.bithon.agent.plugin.apache.kafka.producer.interceptor;

import org.apache.kafka.clients.producer.internals.ProducerBatch;
import org.apache.kafka.common.TopicPartition;
import org.bithon.agent.bootstrap.aop.AbstractInterceptor;
import org.bithon.agent.bootstrap.aop.AopContext;
import org.bithon.agent.observability.metric.collector.MetricRegistryFactory;
import org.bithon.agent.plugin.apache.kafka.KafkaPluginContext;
import org.bithon.agent.plugin.apache.kafka.producer.metrics.ProducerMetricRegistry;
import org.bithon.agent.plugin.apache.kafka.producer.metrics.ProducerMetrics;
import org.bithon.component.commons.utils.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

/**
 * {@link org.apache.kafka.clients.producer.internals.Sender$SenderMetrics}
 *
 * @author frank.chen021@outlook.com
 * @date 2022/12/3 16:36
 */
public class SenderMetrics$UpdateProduceRequestMetrics extends AbstractInterceptor {

    private ProducerMetricRegistry metricRegistry;
    private Field topicPartitionField;
    private Field recordCountField;
    private Field maxRecordSizeField;

    @Override
    public boolean initialize() {
        metricRegistry = MetricRegistryFactory.getOrCreateRegistry("kafka-producer",
                                                                   ProducerMetricRegistry::new);

        try {
            topicPartitionField = ReflectionUtils.getField(ProducerBatch.class, "topicPartition");
            topicPartitionField.setAccessible(true);
        } catch (NoSuchFieldException ignored) {
        }

        try {
            recordCountField = ReflectionUtils.getField(ProducerBatch.class, "recordCount");
            recordCountField.setAccessible(true);
        } catch (NoSuchFieldException ignored) {
        }

        try {
            maxRecordSizeField = ReflectionUtils.getField(ProducerBatch.class, "maxRecordSize");
            maxRecordSizeField.setAccessible(true);
        } catch (NoSuchFieldException ignored) {
        }

        return true;
    }

    @Override
    public void onMethodLeave(AopContext aopContext) {
        Map<Integer, List<ProducerBatch>> batches = aopContext.getArgAs(0);
        if (batches.isEmpty()) {
            return;
        }

        KafkaPluginContext producerCtx = aopContext.getInjectedOnTargetAs();

        for (Map.Entry<Integer, List<ProducerBatch>> entry : batches.entrySet()) {
            String nodeId = entry.getKey().toString();
            List<ProducerBatch> batchForNode = entry.getValue();

            for (ProducerBatch batch : batchForNode) {
                try {
                    TopicPartition topicPartition = (TopicPartition) topicPartitionField.get(batch);
                    int recordCount = (int) recordCountField.get(batch);
                    int maxRecordSize = (int) maxRecordSizeField.get(batch);

                    ProducerMetrics metrics = metricRegistry.getOrCreateMetrics(producerCtx.clusterSupplier.get(),
                                                                                nodeId,
                                                                                topicPartition.topic(),
                                                                                producerCtx.clientId);

                    // TODO: 0.11 has different method name
                    metrics.batchSize.update(batch.estimatedSizeInBytes());

                    metrics.maxRecordBytes.update(maxRecordSize);
                    metrics.recordCount.update(recordCount);

                } catch (IllegalAccessException ignored) {
                }
            }

            // record request count
        }
    }
}

