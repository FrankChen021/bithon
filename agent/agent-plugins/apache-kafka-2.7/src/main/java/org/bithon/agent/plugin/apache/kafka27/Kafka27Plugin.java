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

package org.bithon.agent.plugin.apache.kafka27;

import org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptor;
import org.bithon.agent.instrumentation.aop.interceptor.plugin.IPlugin;
import org.bithon.agent.instrumentation.aop.interceptor.precondition.PropertyFileValuePrecondition;
import org.bithon.shaded.net.bytebuddy.description.modifier.Visibility;

import java.util.Collections;
import java.util.List;

import static org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptorBuilder.forClass;

/**
 * @author frankchen
 */
public class Kafka27Plugin implements IPlugin {

    @Override
    public List<InterceptorDescriptor> getInterceptors() {

        return Collections.singletonList(
            forClass("org.apache.kafka.clients.consumer.KafkaConsumer")
                //
                // Only Kafka 2.7.x changes the declaration of {@link org.apache.kafka.clients.consumer.KafkaConsumer}
                //
                .when(new PropertyFileValuePrecondition(
                    "kafka/kafka-version.properties",
                    "version",
                    PropertyFileValuePrecondition.and(
                        PropertyFileValuePrecondition.VersionGT.of("2.7"),
                        PropertyFileValuePrecondition.VersionLT.of("2.8")
                    )
                ))
                .onConstructor()
                .andRawArgs(
                    "java.util.Map",
                    "org.apache.kafka.common.serialization.Deserializer",
                    "org.apache.kafka.common.serialization.Deserializer"
                )
                .interceptedBy("org.bithon.agent.plugin.apache.kafka27.consumer.interceptor.KafkaConsumer$Ctor")

                .onMethod("poll")
                .andVisibility(Visibility.PUBLIC)
                .interceptedBy("org.bithon.agent.plugin.apache.kafka.consumer.interceptor.KafkaConsumer$Poll")

                .build()
        );
    }
}
