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
import org.bithon.agent.bootstrap.aop.IBithonObject;
import org.bithon.agent.bootstrap.aop.context.AopContext;
import org.bithon.agent.bootstrap.aop.interceptor.AfterInterceptor;
import org.bithon.agent.plugin.apache.kafka.KafkaPluginContext;
import org.bithon.component.commons.utils.ReflectionUtils;

import java.util.List;

/**
 * {@link org.apache.kafka.clients.consumer.KafkaConsumer}
 *
 * @author frank.chen021@outlook.com
 * @date 2022/11/16 11:32
 */
public class KafkaConsumer$Ctor extends AfterInterceptor {

    @Override
    public void after(AopContext aopContext) {
        ConsumerConfig consumerConfig = aopContext.getArgAs(0);
        String groupId = consumerConfig.getString(ConsumerConfig.GROUP_ID_CONFIG);
        String clientId = (String) ReflectionUtils.getFieldValue(aopContext.getTarget(), "clientId");

        List<String> bootstrapServers = consumerConfig.getList(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG);
        String boostrapServer = bootstrapServers.stream()
                                                .sorted()
                                                .findFirst()
                                                .get();

        KafkaPluginContext kafkaPluginContext = new KafkaPluginContext();
        kafkaPluginContext.groupId = groupId;
        kafkaPluginContext.clientId = clientId;
        kafkaPluginContext.clusterSupplier = () -> boostrapServer;

        Fetcher<?, ?> fetcher = (Fetcher<?, ?>) ReflectionUtils.getFieldValue(aopContext.getTarget(), "fetcher");
        if (fetcher instanceof IBithonObject) {
            ((IBithonObject) fetcher).setInjectedObject(kafkaPluginContext);
        }

        IBithonObject kafkaConsumer = aopContext.getTargetAs();
        kafkaConsumer.setInjectedObject(kafkaPluginContext);

        ConsumerNetworkClient consumerNetworkClient = (ConsumerNetworkClient) ReflectionUtils.getFieldValue(aopContext.getTarget(), "client");
        Object kafkaNetworkClient = ReflectionUtils.getFieldValue(consumerNetworkClient, "client");
        if (kafkaNetworkClient instanceof IBithonObject) {
            ((IBithonObject) kafkaNetworkClient).setInjectedObject(kafkaPluginContext);
        }
    }
}
