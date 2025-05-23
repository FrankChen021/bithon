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

package org.bithon.agent.plugin.apache.kafka37.consumer.interceptor;

import org.bithon.agent.instrumentation.aop.IBithonObject;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AfterInterceptor;
import org.bithon.agent.plugin.apache.kafka.KafkaPluginContext;
import org.bithon.component.commons.utils.ReflectionUtils;

/**
 * {@link org.apache.kafka.clients.consumer.KafkaConsumer}
 *
 * @author frank.chen021@outlook.com
 * @date 28/1/25 1:15 pm
 */
public class KafkaConsumer$Ctor extends AfterInterceptor {
    @Override
    public void after(AopContext aopContext) {
        if (aopContext.getInjectedOnTargetAs() != null) {
            // The interceptor is called recursively, which means that the ctor is being called by itself
            return;
        }

        Object deleteConsumer = ReflectionUtils.getFieldValue(aopContext.getTarget(), "delegate");
        if (deleteConsumer instanceof IBithonObject) {
            // This delegate consumer is LegacyKafkaConsumer which has been instrumented by LegacyKafkaConsumer$Ctor
            KafkaPluginContext pluginContext = (KafkaPluginContext) ((IBithonObject) deleteConsumer).getInjectedObject();

            // Set the plugin context to the current consumer,
            // which can be accessed by Spring Kafka Consumer interceptors
            IBithonObject consumer = aopContext.getTargetAs();
            consumer.setInjectedObject(pluginContext);
        }
    }
}
