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
                .hook()
                .onConstructor(
                    "org.apache.kafka.clients.consumer.ConsumerConfig",
                    "org.apache.kafka.common.serialization.Deserializer<K>",
                    "org.apache.kafka.common.serialization.Deserializer<V>")
                .to("org.bithon.agent.plugin.apache.kafka.consumer.interceptor.KafkaConsumer$Ctor")

                // tracing
                .hook()
                .onMethod(Matchers.name("poll").and(Matchers.visibility(Visibility.PRIVATE)))
                .to("org.bithon.agent.plugin.apache.kafka.consumer.interceptor.KafkaConsumer$Poll")
                .build(),

            forClass("org.apache.kafka.clients.consumer.internals.Fetcher")
                .hook()
                // Since 0.11
                .onMethodAndArgs("parseRecord",
                                 "org.apache.kafka.common.TopicPartition",
                                 "org.apache.kafka.common.record.RecordBatch",
                                 "org.apache.kafka.common.record.Record")
                .to("org.bithon.agent.plugin.apache.kafka.consumer.interceptor.Fetcher$ParseRecord")
                .build(),

            // Spring Kafka, can move to an independent plugin
            forClass("org.springframework.kafka.listener.KafkaMessageListenerContainer$ListenerConsumer")
                .hook()
                .onAllConstructor()
                .to("org.bithon.agent.plugin.apache.kafka.consumer.interceptor.ListenerConsumer$Ctor")

                .hook()
                .onMethodAndNoArgs("pollAndInvoke")
                .to("org.bithon.agent.plugin.apache.kafka.consumer.interceptor.ListenerConsumer$PollAndInvoke")
                .build(),

            forClass("org.apache.kafka.clients.producer.KafkaProducer")
                .hook()
                .onConstructor(Matchers.visibility(Visibility.PACKAGE_PRIVATE).or(Matchers.visibility(Visibility.PRIVATE)))
                .to("org.bithon.agent.plugin.apache.kafka.producer.interceptor.KafkaProducer$Ctor")

                // tracing
                .hook()
                .onMethodName("doSend")
                .to("org.bithon.agent.plugin.apache.kafka.producer.interceptor.KafkaProducer$DoSend")
                .build(),

            // Producer metrics helper
            forClass("org.apache.kafka.clients.producer.internals.Sender")
                .hook()
                .onMethod(Matchers.name("handleProduceResponse")
                                  .and(Matchers.takesFirstArgument("org.apache.kafka.clients.ClientResponse")))
                .to("org.bithon.agent.plugin.apache.kafka.producer.interceptor.Sender$HandleProduceResponse")
                .build(),

            // Producer metrics
            forClass("org.apache.kafka.clients.producer.internals.Sender$SenderMetrics")
                .hook()
                .onMethodName("updateProduceRequestMetrics")
                .to("org.bithon.agent.plugin.apache.kafka.producer.interceptor.SenderMetrics$UpdateProduceRequestMetrics")

                .hook()
                .onMethodName("recordRetries")
                .to("org.bithon.agent.plugin.apache.kafka.producer.interceptor.SenderMetrics$RecordRetries")

                .hook()
                .onMethodName("recordErrors")
                .to("org.bithon.agent.plugin.apache.kafka.producer.interceptor.SenderMetrics$RecordErrors")
                .build(),

            forClass("org.apache.kafka.clients.NetworkClient")
                .hook()
                .onMethodName("completeResponses")
                .to("org.bithon.agent.plugin.apache.kafka.network.interceptor.NetworkClient$CompleteResponses")
                .build(),

            // AdminClient
            forClass("org.apache.kafka.clients.admin.AdminClient")
                .hook()
                .onMethodName("create")
                .to("org.bithon.agent.plugin.apache.kafka.admin.interceptor.AdminClient$Create")
                .build(),

            forClass("org.apache.kafka.clients.admin.KafkaAdminClient")
                .hook()
                .onMethod(Matchers.implement("org.apache.kafka.clients.admin.Admin"))
                .to("org.bithon.agent.plugin.apache.kafka.admin.interceptor.KafkaAdminClient$All")
                .build()
        );
    }
}
