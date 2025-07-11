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
import org.bithon.agent.instrumentation.aop.interceptor.matcher.Matchers;
import org.bithon.agent.instrumentation.aop.interceptor.plugin.IPlugin;
import org.bithon.agent.instrumentation.aop.interceptor.precondition.PropertyFileValuePrecondition;
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
                .when(
                    // From 3.7.0, the KafkaConsumer class is delegated to LegacyKafkaConsumer internally
                    // The instrumentation is moved to Kafka37Plugin and later
                    new PropertyFileValuePrecondition("kafka/kafka-version.properties",
                                                      "version",
                                                      PropertyFileValuePrecondition.and(
                                                          // 3.7 and above has different implementation of KafkaConsumer
                                                          PropertyFileValuePrecondition.VersionLT.of("3.7.0"),

                                                          // 2.7.x changed the constructor signature of KafkaConsumer
                                                          PropertyFileValuePrecondition.not(
                                                              PropertyFileValuePrecondition.and(
                                                                  PropertyFileValuePrecondition.VersionGT.of("2.7"),
                                                                  PropertyFileValuePrecondition.VersionLT.of("2.8")
                                                              )
                                                          )
                                                      ))
                )

                .onConstructor()
                .andArgs("org.apache.kafka.clients.consumer.ConsumerConfig",
                         "org.apache.kafka.common.serialization.Deserializer<K>",
                         "org.apache.kafka.common.serialization.Deserializer<V>")
                .interceptedBy("org.bithon.agent.plugin.apache.kafka.consumer.interceptor.KafkaConsumer$Ctor")

                .onMethod("poll")
                .andVisibility(Visibility.PUBLIC)
                .interceptedBy("org.bithon.agent.plugin.apache.kafka.consumer.interceptor.KafkaConsumer$Poll")
                .build(),

            // Metric
            forClass("org.apache.kafka.clients.consumer.internals.Fetcher$FetchResponseMetricAggregator")
                .onConstructor()
                .interceptedBy("org.bithon.agent.plugin.apache.kafka.consumer.interceptor.FetchResponseMetricAggregator$Ctor")

                .onMethod("record")
                .andArgsSize(3)
                .interceptedBy("org.bithon.agent.plugin.apache.kafka.consumer.interceptor.FetchResponseMetricAggregator$Record")
                .build(),

            // Since 3.5, the Fetcher$FetchResponseMetricAggregator in the previous release is renamed to FetchMetricsAggregator
            forClass("org.apache.kafka.clients.consumer.internals.FetchMetricsAggregator")
                .onConstructor()
                .interceptedBy("org.bithon.agent.plugin.apache.kafka.consumer.interceptor.FetchMetricsAggregator$Ctor")

                .onMethod("record")
                .andArgsSize(3)
                .interceptedBy("org.bithon.agent.plugin.apache.kafka.consumer.interceptor.FetchMetricsAggregator$Record")
                .build(),

            // Spring Kafka, can move to an independent plugin
            forClass("org.springframework.kafka.listener.KafkaMessageListenerContainer$ListenerConsumer")
                .onConstructor()
                .interceptedBy("org.bithon.agent.plugin.apache.kafka.consumer.interceptor.ListenerConsumer$Ctor")

                .onMethod("pollAndInvoke")
                .andNoArgs()
                .interceptedBy("org.bithon.agent.plugin.apache.kafka.consumer.interceptor.ListenerConsumer$PollAndInvoke")
                .build(),

            forClass("org.apache.kafka.clients.producer.KafkaProducer")
                .onConstructor()
                .andVisibility(Visibility.PACKAGE_PRIVATE, Visibility.PRIVATE)
                .interceptedBy("org.bithon.agent.plugin.apache.kafka.producer.interceptor.KafkaProducer$Ctor")

                // tracing
                .onMethod("doSend")
                .interceptedBy("org.bithon.agent.plugin.apache.kafka.producer.interceptor.KafkaProducer$DoSend")
                .build(),

            // Producer metrics helper
            forClass("org.apache.kafka.clients.producer.internals.Sender")
                .onMethod("handleProduceResponse")
                .andArgs(0, "org.apache.kafka.clients.ClientResponse")
                .interceptedBy("org.bithon.agent.plugin.apache.kafka.producer.interceptor.Sender$HandleProduceResponse")
                .build(),

            // Producer metrics
            forClass("org.apache.kafka.clients.producer.internals.Sender$SenderMetrics")
                .onMethod("updateProduceRequestMetrics")
                .interceptedBy("org.bithon.agent.plugin.apache.kafka.producer.interceptor.SenderMetrics$UpdateProduceRequestMetrics")

                .onMethod("recordRetries")
                .interceptedBy("org.bithon.agent.plugin.apache.kafka.producer.interceptor.SenderMetrics$RecordRetries")

                .onMethod("recordErrors")
                .interceptedBy("org.bithon.agent.plugin.apache.kafka.producer.interceptor.SenderMetrics$RecordErrors")
                .build(),

            forClass("org.apache.kafka.clients.NetworkClient")
                .when(new PropertyFileValuePrecondition("kafka/kafka-version.properties",
                                                        "version",
                                                        PropertyFileValuePrecondition.VersionGTE.of("1.0.0")))
                .onMethod("completeResponses")
                .interceptedBy("org.bithon.agent.plugin.apache.kafka.network.interceptor.NetworkClient$CompleteResponses")
                .build(),

            // AdminClient
            forClass("org.apache.kafka.clients.admin.AdminClient")
                .onMethod("create")
                .interceptedBy("org.bithon.agent.plugin.apache.kafka.admin.interceptor.AdminClient$Create")
                .build(),

            forClass("org.apache.kafka.clients.admin.KafkaAdminClient")
                .onMethod(Matchers.implement("org.apache.kafka.clients.admin.Admin"))
                .interceptedBy("org.bithon.agent.plugin.apache.kafka.admin.interceptor.KafkaAdminClient$All")
                .build()
        );
    }
}
