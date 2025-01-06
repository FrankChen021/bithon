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

package org.bithon.agent.plugin.apache.kafka39;

import org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptor;
import org.bithon.agent.instrumentation.aop.interceptor.plugin.IPlugin;
import org.bithon.agent.instrumentation.aop.interceptor.precondition.IInterceptorPrecondition;
import org.bithon.shaded.net.bytebuddy.description.modifier.Visibility;

import java.util.Collections;
import java.util.List;

import static org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptorBuilder.forClass;

/**
 * @author frankchen
 */
public class Kafka39Plugin implements IPlugin {

    @Override
    public IInterceptorPrecondition getPreconditions() {
        return IInterceptorPrecondition.isClassDefined("org.apache.kafka.clients.consumer.internals.ClassicKafkaConsumer");
    }

    @Override
    public List<InterceptorDescriptor> getInterceptors() {
        return Collections.singletonList(
            forClass("org.apache.kafka.clients.consumer.internals.ClassicKafkaConsumer")
                .onConstructor()
                .andArgs("org.apache.kafka.clients.consumer.ConsumerConfig",
                         "org.apache.kafka.common.serialization.Deserializer<K>",
                         "org.apache.kafka.common.serialization.Deserializer<V>")
                .interceptedBy("org.bithon.agent.plugin.apache.kafka39.consumer.interceptor.ClassicKafkaConsumer$Ctor")

                .onMethod("poll")
                .andVisibility(Visibility.PUBLIC)
                .interceptedBy("org.bithon.agent.plugin.apache.kafka.consumer.interceptor.KafkaConsumer$Poll")
                .build()
        );
    }
}
