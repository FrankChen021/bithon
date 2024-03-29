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

package org.bithon.agent.plugin.apache.kafka.producer.interceptor;

import org.apache.kafka.clients.ClientResponse;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.InterceptionDecision;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AroundInterceptor;
import org.bithon.agent.plugin.apache.kafka.KafkaPluginContext;

import java.util.Map;

/**
 * {@link org.apache.kafka.clients.producer.internals.Sender#handleProduceResponse(ClientResponse, Map, long)}
 *
 * @author frank.chen021@outlook.com
 * @date 2022/12/3 20:24
 */
public class Sender$HandleProduceResponse extends AroundInterceptor {

    @Override
    public InterceptionDecision before(AopContext aopContext) {
        ClientResponse clientResponse = aopContext.getArgAs(0);
        KafkaPluginContext.setCurrentDestination(clientResponse.destination());

        return InterceptionDecision.CONTINUE;
    }

    @Override
    public void after(AopContext aopContext) {
        KafkaPluginContext.resetCurrentDestination();
    }
}
