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

package com.sbss.bithon.agent.dispatcher.brpc;

import com.sbss.bithon.agent.controller.AgentControllerConfig;
import com.sbss.bithon.agent.controller.IAgentController;
import com.sbss.bithon.agent.core.context.AgentContext;
import com.sbss.bithon.agent.core.context.AppInstance;
import com.sbss.bithon.agent.rpc.brpc.ApplicationType;
import com.sbss.bithon.agent.rpc.brpc.BrpcMessageHeader;
import com.sbss.bithon.agent.rpc.brpc.setting.ISettingFetcher;
import com.sbss.bithon.component.brpc.channel.ClientChannel;
import com.sbss.bithon.component.brpc.endpoint.EndPoint;
import com.sbss.bithon.component.brpc.endpoint.RoundRobinEndPointProvider;
import com.sbss.bithon.component.brpc.exception.ClientSideException;
import com.sbss.bithon.component.brpc.exception.ServerSideException;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/6/28 10:41 上午
 */
public class BrpcAgentController implements IAgentController {
    private static final Logger log = LoggerFactory.getLogger(BrpcAgentController.class);

    private final ClientChannel channel;
    private final ISettingFetcher fetcher;

    public BrpcAgentController(AgentControllerConfig config) {
        List<EndPoint> endpoints = Stream.of(config.getServers().split(",")).map(hostAndPort -> {
            String[] parts = hostAndPort.split(":");
            return new EndPoint(parts[0], Integer.parseInt(parts[1]));
        }).collect(Collectors.toList());

        channel = new ClientChannel(new RoundRobinEndPointProvider(endpoints))
            .applicationName(AgentContext.getInstance().getAppInstance().getAppName())
            .configureRetry(30, Duration.ofSeconds(2));

        fetcher = channel.getRemoteService(ISettingFetcher.class);
    }

    @Override
    public Map<String, String> fetch(String appName, String env, long lastModifiedSince) {
        AppInstance appInstance = AgentContext.getInstance().getAppInstance();
        BrpcMessageHeader header = BrpcMessageHeader.newBuilder()
                                                    .setAppName(appInstance.getRawAppName())
                                                    .setEnv(appInstance.getEnv())
                                                    .setInstanceName(appInstance.getHostIp()
                                                                     + ":"
                                                                     + appInstance.getPort())
                                                    .setHostIp(appInstance.getHostIp())
                                                    .setPort(appInstance.getPort())
                                                    .setAppType(ApplicationType.JAVA)
                                                    .build();
        try {
            return fetcher.fetch(header, lastModifiedSince);
        } catch (ClientSideException e) {
            //suppress client exception
            log.error("Failed to fetch settings: {}", e.getMessage());
            return null;
        } catch (ServerSideException e) {
            //suppress stack trace since this exception occurs at server side
            log.error("Failed to fetch settings due to server side exception:\n {}", e.getMessage());
            return null;
        }
    }

    @Override
    public void attachCommands(Object... commands) {
        for (Object cmd : commands) {
            channel.bindService(cmd);
        }
    }
}
