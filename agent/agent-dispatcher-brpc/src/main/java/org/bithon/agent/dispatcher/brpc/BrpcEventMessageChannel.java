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

package org.bithon.agent.dispatcher.brpc;

import org.bithon.agent.core.context.AgentContext;
import org.bithon.agent.core.context.AppInstance;
import org.bithon.agent.core.dispatcher.channel.IMessageChannel;
import org.bithon.agent.core.dispatcher.config.DispatcherConfig;
import org.bithon.agent.rpc.brpc.ApplicationType;
import org.bithon.agent.rpc.brpc.BrpcMessageHeader;
import org.bithon.agent.rpc.brpc.event.BrpcEventMessage;
import org.bithon.agent.rpc.brpc.event.IEventCollector;
import org.bithon.component.brpc.channel.ClientChannel;
import org.bithon.component.brpc.endpoint.EndPoint;
import org.bithon.component.brpc.endpoint.RoundRobinEndPointProvider;
import org.bithon.component.brpc.exception.CallerSideException;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/6/27 20:14
 */
public class BrpcEventMessageChannel implements IMessageChannel {
    private static final Logger log = LoggerFactory.getLogger(BrpcEventMessageChannel.class);

    private final DispatcherConfig dispatcherConfig;
    private final IEventCollector eventCollector;
    private BrpcMessageHeader header;

    public BrpcEventMessageChannel(DispatcherConfig dispatcherConfig) {
        List<EndPoint> endpoints = Stream.of(dispatcherConfig.getServers().split(",")).map(hostAndPort -> {
            String[] parts = hostAndPort.split(":");
            return new EndPoint(parts[0], Integer.parseInt(parts[1]));
        }).collect(Collectors.toList());
        eventCollector = new ClientChannel(new RoundRobinEndPointProvider(endpoints))
            .configureRetry(3, Duration.ofMillis(200))
            .getRemoteService(IEventCollector.class);

        this.dispatcherConfig = dispatcherConfig;

        AppInstance appInstance = AgentContext.getInstance().getAppInstance();
        this.header = BrpcMessageHeader.newBuilder()
                                       .setAppName(appInstance.getQualifiedAppName())
                                       .setEnv(appInstance.getEnv())
                                       .setInstanceName(appInstance.getHostAndPort())
                                       .setHostIp(appInstance.getHostIp())
                                       .setPort(appInstance.getPort())
                                       .setAppType(ApplicationType.JAVA)
                                       .build();
        appInstance.addListener(port -> this.header = BrpcMessageHeader.newBuilder()
                                                                       .setAppName(appInstance.getQualifiedAppName())
                                                                       .setEnv(appInstance.getEnv())
                                                                       .setInstanceName(appInstance.getHostAndPort())
                                                                       .setHostIp(appInstance.getHostIp())
                                                                       .setPort(appInstance.getPort())
                                                                       .setAppType(ApplicationType.JAVA)
                                                                       .build());
    }

    @Override
    public void sendMessage(Object message) {
        if (!(message instanceof BrpcEventMessage)) {
            return;
        }

        boolean isDebugOn = this.dispatcherConfig.getMessageDebug()
                                                 .getOrDefault(BrpcEventMessage.class.getName(), false);
        if (isDebugOn) {
            log.info("[Debugging] Sending Event Messages: {}", message);
        }

        try {
            eventCollector.sendEvent(header, (BrpcEventMessage) message);
        } catch (CallerSideException e) {
            //suppress client exception
            log.error("Failed to send event: {}", e.getMessage());
        }
    }
}
