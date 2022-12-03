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

package org.bithon.agent.plugin.kafka;

import org.bithon.agent.core.aop.descriptor.InterceptorDescriptor;
import org.bithon.agent.core.aop.descriptor.MethodPointCutDescriptorBuilder;
import org.bithon.agent.core.aop.matcher.Matchers;
import org.bithon.agent.core.plugin.IPlugin;
import shaded.net.bytebuddy.description.modifier.Visibility;

import java.util.Arrays;
import java.util.List;

import static org.bithon.agent.core.aop.descriptor.InterceptorDescriptorBuilder.forClass;

/**
 * @author frankchen
 */
public class KafkaPlugin implements IPlugin {

    @Override
    public List<InterceptorDescriptor> getInterceptors() {

        return Arrays.asList(
            forClass("org.apache.kafka.clients.consumer.KafkaConsumer")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onConstructor(
                                                       "org.apache.kafka.clients.consumer.ConsumerConfig",
                                                       "org.apache.kafka.common.serialization.Deserializer<K>",
                                                       "org.apache.kafka.common.serialization.Deserializer<V>")
                                                   .to("org.bithon.agent.plugin.kafka.consumer.interceptor.KafkaConsumer$Ctor"),

                    // tracing
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethod(Matchers.withName("poll").and(Matchers.visibility(Visibility.PRIVATE)))
                                                   .to("org.bithon.agent.plugin.kafka.consumer.interceptor.KafkaConsumer$Poll"),

                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndNoArgs("close")
                                                   .to("org.bithon.agent.plugin.kafka.consumer.interceptor.KafkaConsumer$Close")
                ),

            forClass("org.apache.kafka.clients.consumer.internals.Fetcher")
                .methods(
                    // Since 0.11
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndArgs("parseRecord",
                                                                    "org.apache.kafka.common.TopicPartition",
                                                                    "org.apache.kafka.common.record.RecordBatch",
                                                                    "org.apache.kafka.common.record.Record")
                                                   .to("org.bithon.agent.plugin.kafka.consumer.interceptor.Fetcher010$ParseRecord")
                ),

            // Spring Kafka, can move to an independent plugin
            forClass("org.springframework.kafka.listener.KafkaMessageListenerContainer$ListenerConsumer")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onAllConstructor()
                                                   .to("org.bithon.agent.plugin.kafka.consumer.interceptor.ListenerConsumer$PollAndInvoke"),

                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndNoArgs("pollAndInvoke")
                                                   .to("org.bithon.agent.plugin.kafka.consumer.interceptor.ListenerConsumer$PollAndInvoke")
                ),

            forClass("org.apache.kafka.clients.producer.KafkaProducer")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onConstructor(Matchers.visibility(Visibility.PACKAGE_PRIVATE).or(Matchers.visibility(Visibility.PRIVATE)))
                                                   .to("org.bithon.agent.plugin.kafka.producer.interceptor.KafkaProducer$Ctor"),

                    MethodPointCutDescriptorBuilder.build()
                                                   .onAllMethods("doSend")
                                                   .to("org.bithon.agent.plugin.kafka.producer.interceptor.KafkaProducer$DoSend"),

                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndNoArgs("close")
                                                   .to("org.bithon.agent.plugin.kafka.producer.interceptor.KafkaProducer$Close")
                )
        );
    }

}
