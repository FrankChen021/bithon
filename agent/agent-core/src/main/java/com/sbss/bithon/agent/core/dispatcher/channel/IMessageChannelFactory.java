package com.sbss.bithon.agent.core.dispatcher.channel;

import com.sbss.bithon.agent.core.config.DispatcherConfig;
import com.sbss.bithon.agent.core.dispatcher.IMessageConverter;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/5 11:08 下午
 */
public interface IMessageChannelFactory {
    IMessageChannel createMetricsChannel(DispatcherConfig dispatcherConfig);
    IMessageChannel createTracingChannel(DispatcherConfig dispatcherConfig);
    IMessageChannel createEventsChannel(DispatcherConfig dispatcherConfig);

    IMessageConverter createMessageConverter();
}
