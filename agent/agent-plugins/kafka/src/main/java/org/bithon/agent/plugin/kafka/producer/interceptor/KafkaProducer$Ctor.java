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

package org.bithon.agent.plugin.kafka.producer.interceptor;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.internals.Sender;
import org.bithon.agent.bootstrap.aop.AbstractInterceptor;
import org.bithon.agent.bootstrap.aop.AopContext;
import org.bithon.agent.bootstrap.aop.IBithonObject;
import org.bithon.agent.plugin.kafka.producer.ProducerContext;
import org.bithon.component.commons.utils.ReflectionUtils;

import java.util.List;
import java.util.Properties;

/**
 * {@link org.apache.kafka.clients.producer.KafkaProducer#KafkaProducer(Properties)}
 *
 * @author frank.chen021@outlook.com
 * @date 2022/11/16 11:44
 */
public class KafkaProducer$Ctor extends AbstractInterceptor {

    @Override
    public void onConstruct(AopContext aopContext) {
        ProducerConfig producerConfig = (ProducerConfig) ReflectionUtils.getFieldValue(aopContext.getTarget(), "producerConfig");

        List<String> bootstrapServers = producerConfig.getList(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG);
        String boostrapServer = bootstrapServers.stream()
                                                .sorted()
                                                .findFirst()
                                                .get();

        ProducerContext ctx = new ProducerContext();
        ctx.clientId = (String) ReflectionUtils.getFieldValue(aopContext.getTarget(), "clientId");
        ctx.clusterSupplier = () -> boostrapServer;
        ((IBithonObject) aopContext.getTarget()).setInjectedObject(ctx);

        //
        // set context so that related interceptors can access these infos
        //
        Sender sender = (Sender) ReflectionUtils.getFieldValue(aopContext.getTarget(), "sender");
        Object senderMetrics = ReflectionUtils.getFieldValue(sender, "sensors");
        ((IBithonObject) senderMetrics).setInjectedObject(ctx);
    }
}
