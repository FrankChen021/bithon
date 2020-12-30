package com.sbss.bithon.agent.dispatcher.thrift;

import com.sbss.bithon.agent.core.config.DispatcherConfig;
import com.sbss.bithon.agent.core.dispatcher.channel.IMessageChannel;
import com.sbss.bithon.agent.rpc.thrift.service.event.IEventCollector;
import com.sbss.bithon.agent.rpc.thrift.service.event.ThriftEventMessage;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TProtocol;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/14 3:46 下午
 */
public class ThriftEventMessageChannel implements IMessageChannel {

    private static final int MAX_RETRY = 3;

    private final AbstractThriftClient<IEventCollector.Client> client;

    public ThriftEventMessageChannel(DispatcherConfig dispatcherConfig) {
        client = new AbstractThriftClient<IEventCollector.Client>("events",
                                                                   dispatcherConfig.getServers(),
                                                                   dispatcherConfig.getClient().getTimeout()) {
            @Override
            protected IEventCollector.Client createClient(TProtocol protocol) {
                return new IEventCollector.Client(protocol);
            }
        };
    }

    @Override
    public void sendMessage(Object message) {
        client.ensureClient((client) -> {
            try {
                client.sendEvent((ThriftEventMessage) message);
                return null;
            } catch (TException e) {
                throw new RuntimeException(e);
            }
        }, MAX_RETRY);
    }
}
