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

package org.bithon.agent.plugin.apache.kafka27.consumer.interceptor;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.bithon.agent.instrumentation.aop.context.AopContext;

import java.util.Map;

/**
 * {@link org.apache.kafka.clients.consumer.KafkaConsumer}
 *
 * @author frank.chen021@outlook.com
 * @date 28/1/25 1:15 pm
 */
public class KafkaConsumer$Ctor extends org.bithon.agent.plugin.apache.kafka.consumer.interceptor.KafkaConsumer$Ctor {
    @Override
    public void after(AopContext aopContext) {

        //noinspection unchecked
        ConsumerConfig consumerConfig = new ConsumerConfig((Map<String, Object>) aopContext.getArgAs(0));

        onCall(consumerConfig, aopContext);
    }
}
