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

package com.sbss.bithon.agent.core.dispatcher.channel;

import com.sbss.bithon.agent.core.config.DispatcherConfig;
import com.sbss.bithon.agent.core.dispatcher.IMessageConverter;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/5 11:08 下午
 */
public interface IMessageChannelFactory {
    IMessageChannel createMetricChannel(DispatcherConfig dispatcherConfig);

    IMessageChannel createTracingChannel(DispatcherConfig dispatcherConfig);

    IMessageChannel createEventChannel(DispatcherConfig dispatcherConfig);

    IMessageConverter createMessageConverter();
}
