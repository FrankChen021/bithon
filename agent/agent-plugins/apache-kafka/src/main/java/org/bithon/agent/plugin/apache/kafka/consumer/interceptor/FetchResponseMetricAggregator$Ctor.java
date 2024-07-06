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
import org.bithon.agent.observability.context.InterceptorContext;
import org.bithon.agent.plugin.apache.kafka.KafkaPluginContext;

/**
 *  * {@link org.apache.kafka.clients.consumer.internals.Fetcher.FetchResponseMetricAggregator}
 *
 * @author frank.chen021@outlook.com
 * @date 6/7/24 11:14 pm
 */
public class FetchResponseMetricAggregator$Ctor extends AfterInterceptor {
    @Override
    public void after(AopContext aopContext) throws Exception {
        KafkaPluginContext kafkaPluginContext = (KafkaPluginContext) InterceptorContext.get("kafka.consumer.context");
        IBithonObject bithonObject = aopContext.getTargetAs();
        bithonObject.setInjectedObject(kafkaPluginContext);
    }
}
