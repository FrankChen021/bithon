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
    private static final int MAX_RETRY = 3;
    static Logger log = LoggerFactory.getLogger(ThriftTracingMessageChannel.class);
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
