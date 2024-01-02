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

package org.bithon.agent.plugin.apache.kafka;

import org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptor;
import org.bithon.agent.instrumentation.aop.interceptor.descriptor.MethodPointCutDescriptorBuilder;
import org.bithon.agent.instrumentation.aop.interceptor.matcher.Matchers;
import org.bithon.agent.instrumentation.aop.interceptor.plugin.IPlugin;
import org.bithon.shaded.net.bytebuddy.description.modifier.Visibility;

import java.util.Arrays;
import java.util.List;

import static org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptorBuilder.forClass;

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
                                                   .to("org.bithon.agent.plugin.apache.kafka.consumer.interceptor.KafkaConsumer$Ctor"),

                    // tracing
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethod(Matchers.withName("poll").and(Matchers.visibility(Visibility.PRIVATE)))
                                                   .to("org.bithon.agent.plugin.apache.kafka.consumer.interceptor.KafkaConsumer$Poll")
                        ),

            forClass("org.apache.kafka.clients.consumer.internals.Fetcher")
                .methods(
                    // Since 0.11
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndArgs("parseRecord",
                                                                    "org.apache.kafka.common.TopicPartition",
                                                                    "org.apache.kafka.common.record.RecordBatch",
                                                                    "org.apache.kafka.common.record.Record")
                                                   .to("org.bithon.agent.plugin.apache.kafka.consumer.interceptor.Fetcher$ParseRecord")
                        ),

            // Spring Kafka, can move to an independent plugin
            forClass("org.springframework.kafka.listener.KafkaMessageListenerContainer$ListenerConsumer")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onAllConstructor()
                                                   .to("org.bithon.agent.plugin.apache.kafka.consumer.interceptor.ListenerConsumer$Ctor"),

                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndNoArgs("pollAndInvoke")
                                                   .to("org.bithon.agent.plugin.apache.kafka.consumer.interceptor.ListenerConsumer$PollAndInvoke")
                        ),

            forClass("org.apache.kafka.clients.producer.KafkaProducer")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onConstructor(Matchers.visibility(Visibility.PACKAGE_PRIVATE).or(Matchers.visibility(Visibility.PRIVATE)))
                                                   .to("org.bithon.agent.plugin.apache.kafka.producer.interceptor.KafkaProducer$Ctor"),

                    // tracing
                    MethodPointCutDescriptorBuilder.build()
                                                   .onAllMethods("doSend")
                                                   .to("org.bithon.agent.plugin.apache.kafka.producer.interceptor.KafkaProducer$DoSend")

                        ),

            // Producer metrics helper
            forClass("org.apache.kafka.clients.producer.internals.Sender")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethod(Matchers.withName("handleProduceResponse")
                                                                     .and(Matchers.takesFirstArgument("org.apache.kafka.clients.ClientResponse")))
                                                   .to("org.bithon.agent.plugin.apache.kafka.producer.interceptor.Sender$HandleProduceResponse")
                        ),

            // Producer metrics
            forClass("org.apache.kafka.clients.producer.internals.Sender$SenderMetrics")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onAllMethods("updateProduceRequestMetrics")
                                                   .to("org.bithon.agent.plugin.apache.kafka.producer.interceptor.SenderMetrics$UpdateProduceRequestMetrics"),

                    MethodPointCutDescriptorBuilder.build()
                                                   .onAllMethods("recordRetries")
                                                   .to("org.bithon.agent.plugin.apache.kafka.producer.interceptor.SenderMetrics$RecordRetries"),

                    MethodPointCutDescriptorBuilder.build()
                                                   .onAllMethods("recordErrors")
                                                   .to("org.bithon.agent.plugin.apache.kafka.producer.interceptor.SenderMetrics$RecordErrors")
                        ),

            forClass("org.apache.kafka.clients.NetworkClient")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onAllMethods("completeResponses")
                                                   .to("org.bithon.agent.plugin.apache.kafka.network.interceptor.NetworkClient$CompleteResponses")
                        ),

            // AdminClient
            forClass("org.apache.kafka.clients.admin.AdminClient")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onAllMethods("create")
                                                   .to("org.bithon.agent.plugin.apache.kafka.admin.interceptor.AdminClient$Create")
                        ),

            forClass("org.apache.kafka.clients.admin.KafkaAdminClient")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethod(Matchers.implement("org.apache.kafka.clients.admin.Admin"))
                                                   .to("org.bithon.agent.plugin.apache.kafka.admin.interceptor.KafkaAdminClient$All")
                        )

                            );
    }
}
