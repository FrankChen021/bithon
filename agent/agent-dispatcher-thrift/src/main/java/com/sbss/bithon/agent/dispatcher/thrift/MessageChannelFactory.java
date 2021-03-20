package com.sbss.bithon.agent.dispatcher.thrift;

import com.sbss.bithon.agent.core.config.DispatcherConfig;
import com.sbss.bithon.agent.core.dispatcher.IMessageConverter;
import com.sbss.bithon.agent.core.dispatcher.channel.IMessageChannel;
import com.sbss.bithon.agent.core.dispatcher.channel.IMessageChannelFactory;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/6 11:38 下午
 */
public class MessageChannelFactory implements IMessageChannelFactory {
    @Override
    public IMessageChannel createMetricChannel(DispatcherConfig dispatcherConfig) {
        return new ThriftMetricsMessageChannel(dispatcherConfig);
    }

    @Override
    public IMessageChannel createTracingChannel(DispatcherConfig dispatcherConfig) {
        return new ThriftTracingMessageChannel(dispatcherConfig);
    }

    @Override
    public IMessageChannel createEventChannel(DispatcherConfig dispatcherConfig) {
        return new ThriftEventMessageChannel(dispatcherConfig);
    }

    @Override
    public IMessageConverter createMessageConverter() {
        return new ToThriftMessageConverter();
    }
}
