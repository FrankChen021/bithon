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

package org.bithon.agent.plugin.kafka.consumer.interceptor;

import org.bithon.agent.bootstrap.aop.AbstractInterceptor;
import org.bithon.agent.bootstrap.aop.AopContext;
import org.bithon.agent.plugin.kafka.consumer.ManagedKafkaConsumers;
import org.bithon.component.commons.utils.ReflectionUtils;

/**
 * @author frank.chen021@outlook.com
 * @date 2022/11/16 11:32
 */
public class KafkaConsumer$Ctor extends AbstractInterceptor {
    @Override
    public void onConstruct(AopContext aopContext) {
        String clientId = (String) ReflectionUtils.getFieldValue(aopContext.getTarget(), "clientId");

        ManagedKafkaConsumers.getInstance()
                             .register(clientId,
                                      aopContext.castTargetAs(),
                                      aopContext.getArgAs(0));

    }
}
