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

package com.sbss.bithon.agent.dispatcher.netty;

import cn.bithon.rpc.channel.ClientChannel;
import cn.bithon.rpc.endpoint.EndPoint;
import cn.bithon.rpc.endpoint.RoundRobinEndPointProvider;
import cn.bithon.rpc.services.ApplicationType;
import cn.bithon.rpc.services.ITraceCollector;
import cn.bithon.rpc.services.MessageHeader;
import cn.bithon.rpc.services.tracing.TraceSpanMessage;
import com.sbss.bithon.agent.core.config.DispatcherConfig;
import com.sbss.bithon.agent.core.context.AgentContext;
import com.sbss.bithon.agent.core.context.AppInstance;
import com.sbss.bithon.agent.core.dispatcher.channel.IMessageChannel;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/6/27 20:14
 */
public class TraceMessageChannel implements IMessageChannel {
    private static final Logger log = LoggerFactory.getLogger(TraceMessageChannel.class);

    private final ITraceCollector traceCollector;
    private final DispatcherConfig dispatcherConfig;
    private MessageHeader header;

    public TraceMessageChannel(DispatcherConfig dispatcherConfig) {

        List<EndPoint> endpoints = Stream.of(dispatcherConfig.getServers().split(",")).map(hostAndPort -> {
            String[] parts = hostAndPort.split(":");
            return new EndPoint(parts[0], Integer.parseInt(parts[1]));
        }).collect(Collectors.toList());
        traceCollector = new ClientChannel(new RoundRobinEndPointProvider(endpoints)).getRemoteService(ITraceCollector.class);

        this.dispatcherConfig = dispatcherConfig;

        AppInstance appInstance = AgentContext.getInstance().getAppInstance();
        this.header = MessageHeader.newBuilder()
                                   .setAppName(appInstance.getAppName())
                                   .setEnv(appInstance.getEnv())
                                   .setInstanceName(appInstance.getHostIp() + ":" + appInstance.getPort())
                                   .setHostIp(appInstance.getHostIp())
                                   .setPort(appInstance.getPort())
                                   .setAppType(ApplicationType.JAVA)
                                   .build();
        appInstance.addListener(port -> this.header = MessageHeader.newBuilder()
                                                                   .setAppName(appInstance.getAppName())
                                                                   .setEnv(appInstance.getEnv())
                                                                   .setInstanceName(appInstance.getHostIp()
                                                                                    + ":"
                                                                                    + appInstance.getPort())
                                                                   .setHostIp(appInstance.getHostIp())
                                                                   .setPort(appInstance.getPort())
                                                                   .setAppType(ApplicationType.JAVA)
                                                                   .build());
    }

    @Override
    public void sendMessage(Object message) {
        if (!(message instanceof List)) {
            return;
        }
        if (((List<?>) message).isEmpty()) {
            return;
        }

        boolean isDebugOn = this.dispatcherConfig.getMessageDebug()
                                                 .getOrDefault(TraceSpanMessage.class.getName(), false);
        if (isDebugOn) {
            log.info("[Debugging] Sending Tracing Messages: {}", message);
        }

        //noinspection unchecked
        this.traceCollector.sendTrace(this.header,
                                      (List<TraceSpanMessage>) message);
    }
}
