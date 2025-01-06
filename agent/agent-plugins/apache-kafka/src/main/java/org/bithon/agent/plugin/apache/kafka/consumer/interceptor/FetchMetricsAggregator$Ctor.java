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

import java.util.Set;

/**
 * Since 3.5
 *
 * {@link org.apache.kafka.clients.consumer.internals.FetchMetricsAggregator#FetchMetricsAggregator(FetchMetricsManager, Set)}
 *
 * @author frank.chen021@outlook.com
 * @date 6/7/24 11:01 pm
 */
public class FetchMetricsAggregator$Ctor extends AfterInterceptor {
    @Override
    public void after(AopContext aopContext) throws Exception {
        IBithonObject bithonObject = aopContext.getTargetAs();

        // KafkaPluginContext is set by xxxKafkaConsumer#poll
        bithonObject.setInjectedObject(KafkaPluginContext.getCurrent());
    }
}
