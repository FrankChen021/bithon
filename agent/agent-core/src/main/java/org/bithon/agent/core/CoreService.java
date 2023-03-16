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

package org.bithon.agent.core;

import org.bithon.agent.core.context.AgentContext;
import org.bithon.agent.core.dispatcher.Dispatcher;
import org.bithon.agent.core.dispatcher.Dispatchers;
import org.bithon.agent.core.metric.collector.jvm.JvmEventMessageBuilder;
import org.bithon.agent.core.metric.collector.jvm.JvmMetricCollector;
import org.bithon.agent.core.starter.IAgentService;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/3/16 23:26
 */
public class CoreService implements IAgentService {
    @Override
    public int getOrder() {
        return Integer.MIN_VALUE;
    }

    @Override
    public void start(AgentContext context) throws Exception {
        new JvmMetricCollector().start();

        //
        // dispatch started message once the dispatcher is ready
        //
        Dispatchers.getOrCreate(Dispatchers.DISPATCHER_NAME_EVENT)
                   .onReady((dispatcher) -> dispatcher.sendMessage(dispatcher.getMessageConverter()
                                                                             .from(JvmEventMessageBuilder.buildJvmStartedEventMessage())));
    }

    @Override
    public void stop() {
        // dispatch jvm stopped message
        Dispatcher dispatcher = Dispatchers.getOrCreate(Dispatchers.DISPATCHER_NAME_EVENT);
        dispatcher.sendMessage(dispatcher.getMessageConverter().from(JvmEventMessageBuilder.buildStoppedEventMessage()));
    }
}
