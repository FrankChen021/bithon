/*
 *    Copyright 2020 bithon.cn
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

package org.bithon.agent.dispatcher.brpc;


import org.bithon.agent.core.dispatcher.IMessageConverter;
import org.bithon.agent.core.dispatcher.channel.IMessageChannel;
import org.bithon.agent.core.dispatcher.channel.IMessageChannelFactory;
import org.bithon.agent.core.dispatcher.config.DispatcherConfig;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/6/27 20:01
 */
public class BrpcChannelFactory implements IMessageChannelFactory {
    @Override
    public IMessageChannel createMetricChannel(DispatcherConfig dispatcherConfig) {
        return new BrpcMetricMessageChannel(dispatcherConfig);
    }

    @Override
    public IMessageChannel createTracingChannel(DispatcherConfig dispatcherConfig) {
        return new BrpcTraceMessageChannel(dispatcherConfig);
    }

    @Override
    public IMessageChannel createEventChannel(DispatcherConfig dispatcherConfig) {
        return new BrpcEventMessageChannel(dispatcherConfig);
    }

    @Override
    public IMessageConverter createMessageConverter() {
        return new BrpcMessageConverter();
    }
}
