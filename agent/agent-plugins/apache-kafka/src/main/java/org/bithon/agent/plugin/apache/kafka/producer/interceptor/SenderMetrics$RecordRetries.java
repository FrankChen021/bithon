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

import org.bithon.agent.bootstrap.aop.AbstractInterceptor;
import org.bithon.agent.bootstrap.aop.AopContext;
import org.bithon.agent.core.metric.collector.MetricRegistryFactory;
import org.bithon.agent.plugin.apache.kafka.KafkaPluginContext;
import org.bithon.agent.plugin.apache.kafka.producer.metrics.ProducerMetricRegistry;

/**
 * @author frank.chen021@outlook.com
 * @date 2022/12/3 16:37
 */
public class SenderMetrics$RecordRetries extends AbstractInterceptor {

    private ProducerMetricRegistry metricRegistry;

    @Override
    public boolean initialize() {
        metricRegistry = MetricRegistryFactory.getOrCreateRegistry("kafka-producer",
                                                                   ProducerMetricRegistry::new);
        return true;
    }

    @Override
    public void onMethodLeave(AopContext aopContext) {
        String topic = aopContext.getArgAs(0);
        int count = aopContext.getArgAs(1);

        KafkaPluginContext producerCtx = aopContext.getInjectedOnTargetAs();
        metricRegistry.getOrCreateMetrics(producerCtx.clusterSupplier.get(),
                                          KafkaPluginContext.getCurrentDestination(),
                                          topic,
                                          producerCtx.clientId).retryRecordCount.update(count);
    }
}
