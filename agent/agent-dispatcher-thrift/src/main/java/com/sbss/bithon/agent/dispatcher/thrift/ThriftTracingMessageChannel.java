package com.sbss.bithon.agent.dispatcher.thrift;

import com.sbss.bithon.agent.core.config.DispatcherConfig;
import com.sbss.bithon.agent.core.dispatcher.channel.IMessageChannel;
import com.sbss.bithon.agent.rpc.thrift.service.trace.TraceCollectorService;
import com.sbss.bithon.agent.rpc.thrift.service.trace.TraceMessage;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TProtocol;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/6 11:40 下午
 */
public class ThriftTracingMessageChannel implements IMessageChannel {
    static Logger log = LoggerFactory.getLogger(ThriftTracingMessageChannel.class);

    private static final int MAX_RETRY = 3;

    private final AbstractThriftClient<TraceCollectorService.Client> client;

    public ThriftTracingMessageChannel(DispatcherConfig dispatcherConfig) {
        client = new AbstractThriftClient<TraceCollectorService.Client>("tracing",
                                                                        dispatcherConfig.getServers(),
                                                                        dispatcherConfig.getClient().getTimeout()) {
            @Override
            protected TraceCollectorService.Client createClient(TProtocol protocol) {
                return new TraceCollectorService.Client(protocol);
            }
        };
    }

    @Override
    public void sendMessage(Object message) {
        // TODO: check timestamp first
        //if (log.isDebugEnabled()) {
            log.info("Sending Trace: {}", message.toString());
        //}
        this.client.ensureClient((client) -> {
            try {
                client.writeTrace((TraceMessage) message);
                return null;
            } catch (TException e) {
                throw new RuntimeException(e);
            }
        }, MAX_RETRY);
    }
}
