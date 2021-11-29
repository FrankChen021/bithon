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

package org.bithon.agent.dispatcher.thrift;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TProtocol;
import org.bithon.agent.core.context.AgentContext;
import org.bithon.agent.core.context.AppInstance;
import org.bithon.agent.core.dispatcher.channel.IMessageChannel;
import org.bithon.agent.core.dispatcher.config.DispatcherConfig;
import org.bithon.agent.rpc.thrift.service.MessageHeader;
import org.bithon.agent.rpc.thrift.service.event.IEventCollector;
import org.bithon.agent.rpc.thrift.service.event.ThriftEventMessage;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/14 3:46 下午
 */
public class ThriftEventMessageChannel implements IMessageChannel {

    private static final int MAX_RETRY = 3;

    private final AbstractThriftClient<IEventCollector.Client> client;
    private final MessageHeader header;

    public ThriftEventMessageChannel(DispatcherConfig dispatcherConfig) {
        client = new AbstractThriftClient<IEventCollector.Client>("event",
                                                                  dispatcherConfig.getServers(),
                                                                  dispatcherConfig.getClient().getTimeout()) {
            @Override
            protected IEventCollector.Client createClient(TProtocol protocol) {
                return new IEventCollector.Client(protocol);
            }
        };

        AppInstance appInstance = AgentContext.getInstance().getAppInstance();
        this.header = new MessageHeader();
        this.header.setAppName(appInstance.getQualifiedAppName());
        this.header.setEnv(appInstance.getEnv());
        this.header.setInstanceName(appInstance.getHostAndPort());
        this.header.setHostIp(appInstance.getHostIp());
        this.header.setPort(appInstance.getPort());
        appInstance.addListener(port -> {
            this.header.setPort(appInstance.getPort());
            this.header.setInstanceName(appInstance.getHostAndPort());
        });
    }

    @Override
    public void sendMessage(Object message) {
        client.ensureClient((client) -> {
            try {
                client.sendEvent(this.header, (ThriftEventMessage) message);
                return null;
            } catch (TException e) {
                throw new RuntimeException(e);
            }
        }, MAX_RETRY);
    }
}
