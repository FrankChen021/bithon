package com.sbss.bithon.agent.dispatcher.thrift;

import com.sbss.bithon.agent.core.config.DispatcherConfig;
import com.sbss.bithon.agent.core.context.AgentContext;
import com.sbss.bithon.agent.core.context.AppInstance;
import com.sbss.bithon.agent.core.dispatcher.channel.IMessageChannel;
import com.sbss.bithon.agent.rpc.thrift.service.MessageHeader;
import com.sbss.bithon.agent.rpc.thrift.service.trace.ITraceCollector;
import com.sbss.bithon.agent.rpc.thrift.service.trace.TraceSpanMessage;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TProtocol;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/6 11:40 下午
 */
public class ThriftTracingMessageChannel implements IMessageChannel {
    static Logger log = LoggerFactory.getLogger(ThriftTracingMessageChannel.class);

    private static final int MAX_RETRY = 3;

    private final AbstractThriftClient<ITraceCollector.Client> client;
    private final MessageHeader header;

    public ThriftTracingMessageChannel(DispatcherConfig dispatcherConfig) {
        client = new AbstractThriftClient<ITraceCollector.Client>("tracing",
                                                                  dispatcherConfig.getServers(),
                                                                  dispatcherConfig.getClient().getTimeout()) {
            @Override
            protected ITraceCollector.Client createClient(TProtocol protocol) {
                return new ITraceCollector.Client(protocol);
            }
        };

        AppInstance appInstance = AgentContext.getInstance().getAppInstance();
        this.header = new MessageHeader();
        this.header.setAppName(appInstance.getAppName());
        this.header.setEnv(appInstance.getEnv());
        this.header.setInstanceName(appInstance.getHostIp() + ":" + appInstance.getPort());
        this.header.setHostIp(appInstance.getHostIp());
        this.header.setPort(appInstance.getPort());
        appInstance.addListener(port -> {
            this.header.setPort(appInstance.getPort());
            this.header.setInstanceName(appInstance.getHostIp() + ":" + appInstance.getPort());
        });
    }

    @Override
    public void sendMessage(Object message) {

        // TODO: check timestamp first
        //if (log.isDebugEnabled()) {
        log.info("Sending Trace: {}", message.toString());
        //}
        this.client.ensureClient((client) -> {
            try {
                client.sendTrace(header, (List<TraceSpanMessage>) message);
                return null;
            } catch (TException e) {
                throw new RuntimeException(e);
            }
        }, MAX_RETRY);
    }
}
