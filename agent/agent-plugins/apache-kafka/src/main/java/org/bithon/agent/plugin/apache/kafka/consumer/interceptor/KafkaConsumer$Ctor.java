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

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.internals.ConsumerNetworkClient;
import org.apache.kafka.clients.consumer.internals.Fetcher;
import org.bithon.agent.instrumentation.aop.IBithonObject;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AfterInterceptor;
import org.bithon.agent.plugin.apache.kafka.KafkaPluginContext;
import org.bithon.component.commons.utils.ReflectionUtils;

/**
 * {@link org.apache.kafka.clients.consumer.KafkaConsumer}
 *
 * @author frank.chen021@outlook.com
 * @date 2022/11/16 11:32
 */
public class KafkaConsumer$Ctor extends AfterInterceptor {

    @Override
    public void after(AopContext aopContext) {
        if (aopContext.hasException()) {
            return;
        }

        ConsumerConfig consumerConfig = aopContext.getArgAs(0);
        String groupId = consumerConfig.getString(ConsumerConfig.GROUP_ID_CONFIG);
        String clientId = (String) ReflectionUtils.getFieldValue(aopContext.getTarget(), "clientId");

        KafkaPluginContext kafkaPluginContext = new KafkaPluginContext();
        kafkaPluginContext.groupId = groupId;
        kafkaPluginContext.clientId = clientId;
        kafkaPluginContext.broker = consumerConfig.getList(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG)
                                                  .stream()
                                                  .sorted()
                                                  .findFirst()
                                                  .get();

        Fetcher<?, ?> fetcher = (Fetcher<?, ?>) ReflectionUtils.getFieldValue(aopContext.getTarget(), "fetcher");
        if (fetcher instanceof IBithonObject) {
            ((IBithonObject) fetcher).setInjectedObject(kafkaPluginContext);
        }

        IBithonObject kafkaConsumer = aopContext.getTargetAs();
        kafkaConsumer.setInjectedObject(kafkaPluginContext);

        ConsumerNetworkClient consumerNetworkClient = (ConsumerNetworkClient) ReflectionUtils.getFieldValue(aopContext.getTarget(), "client");
        if (!setContextOnNetworkClient(kafkaPluginContext, consumerNetworkClient)) {
            // Check if the KafkaConsumer is the type of higher version.
            // The Higher version of the Kafka client wraps the consumer into a 'delegate' property
            Object consumerDelegate = ReflectionUtils.getFieldValue(aopContext.getTarget(), "delegate");
            String clazzName = consumerDelegate.getClass().getSimpleName();
            if ("LegacyKafkaConsumer".equals(clazzName)) {
                if (consumerDelegate instanceof IBithonObject) {
                    // The LegacyKafkaConsumer is also instrumented
                    ((IBithonObject) consumerDelegate).setInjectedObject(kafkaPluginContext);
                }

                setContextOnNetworkClient(kafkaPluginContext, ReflectionUtils.getFieldValue(consumerDelegate, "client"));
            } else if ("AsyncKafkaConsumer".equals(clazzName)) {
                // TODO:
            }
        }
    }

    /**
     * Set context to the {@link org.apache.kafka.clients.NetworkClient} object inside the {@link ConsumerNetworkClient}
     *
     * @param consumerNetworkClient type of {@link ConsumerNetworkClient}
     */
    private boolean setContextOnNetworkClient(KafkaPluginContext ctx, Object consumerNetworkClient) {
        Object kafkaNetworkClient = ReflectionUtils.getFieldValue(consumerNetworkClient, "client");
        if (kafkaNetworkClient instanceof IBithonObject) {
            ((IBithonObject) kafkaNetworkClient).setInjectedObject(ctx);
            return true;
        } else {
            return false;
        }
    }
}
