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

import org.bithon.agent.instrumentation.aop.IBithonObject;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AfterInterceptor;
import org.bithon.agent.plugin.apache.kafka.KafkaPluginContext;
import org.bithon.component.commons.utils.ReflectionUtils;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.TopicPartitionOffset;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * {@link org.springframework.kafka.listener.KafkaMessageListenerContainer$ListenerConsumer}
 *
 * @author Frank Chen
 * @date 28/11/22 8:47 pm
 */
public class ListenerConsumer$Ctor extends AfterInterceptor {

    /**
     * Keep topic information on injected fields for further use
     */
    @Override
    public void after(AopContext aopContext) {
        // The declaration of the 'consumer' field is a type of KafkaConsumer.
        // However, we can't dynamically cast it to KafkaConsumer,
        // because some specific Spring Kafka will apply proxy on this KafkaConsumer, which is a proxy object.
        //
        // For the JDK proxy object, it also has the IBithonObject declaration,
        // the calls on the IBithonObject will be forwarded to the underlying KafkaConsumer object by the mechanism of the JDK proxy,
        // so it's safe to convert it into IBithonObject in the following.
        // See: https://github.com/FrankChen021/bithon/issues/748 to know more.
        Object consumer = ReflectionUtils.getFieldValue(aopContext.getTarget(), "consumer");
        if (consumer == null) {
            return;
        }

        // For KafkaConsumer, the plugin context is injected by interceptors of its CTORs
        // But the injected context might be NULL for Kafka starting from 3.7
        // if the consumer is configured not using the legacy KafkaConsumer
        KafkaPluginContext pluginContext = ((KafkaPluginContext) ((IBithonObject) consumer).getInjectedObject());
        if (pluginContext != null) {
            ContainerProperties properties = (ContainerProperties) ReflectionUtils.getFieldValue(aopContext.getTarget(), "containerProperties");
            if (properties != null) {
                String topicString = null;
                String[] topics = properties.getTopics();
                if (topics != null) {
                    topicString = String.join(",", topics);
                } else if (properties.getTopicPattern() != null) {
                    topicString = properties.getTopicPattern().pattern();
                } else {
                    TopicPartitionOffset[] partitions = properties.getTopicPartitions();
                    if (partitions != null) {
                        topicString = Stream.of(partitions)
                                            .map(TopicPartitionOffset::getTopic)
                                            .collect(Collectors.joining(","));
                    }
                }
                pluginContext.uri = "kafka://" + pluginContext.broker + (topicString == null ? "" : "?topic=" + topicString);
                pluginContext.topic = topicString;
            }
        }

        // Keep the uri for further use
        IBithonObject bithonObject = aopContext.getTargetAs();
        bithonObject.setInjectedObject(pluginContext);
    }
}
