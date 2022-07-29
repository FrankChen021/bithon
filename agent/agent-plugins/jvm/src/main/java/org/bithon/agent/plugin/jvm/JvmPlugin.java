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

package org.bithon.agent.plugin.jvm;

import org.bithon.agent.core.dispatcher.Dispatcher;
import org.bithon.agent.core.dispatcher.Dispatchers;
import org.bithon.agent.core.plugin.IPlugin;

/**
 * @author frankchen
 */
public class JvmPlugin implements IPlugin {

    @Override
    public void start() {
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
