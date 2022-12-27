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

import org.bithon.agent.controller.AgentControllerConfig;
import org.bithon.agent.controller.IAgentController;
import org.bithon.agent.core.context.AgentContext;
import org.bithon.agent.core.context.AppInstance;
import org.bithon.agent.rpc.brpc.ApplicationType;
import org.bithon.agent.rpc.brpc.BrpcMessageHeader;
import org.bithon.agent.rpc.brpc.setting.ISettingFetcher;
import org.bithon.component.brpc.channel.ClientChannel;
import org.bithon.component.brpc.channel.ClientChannelBuilder;
import org.bithon.component.brpc.endpoint.EndPoint;
import org.bithon.component.brpc.endpoint.RoundRobinEndPointProvider;
import org.bithon.component.brpc.exception.CalleeSideException;
import org.bithon.component.brpc.exception.CallerSideException;
import org.bithon.component.brpc.exception.ServiceInvocationException;
import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A controller that accepts and serves commands from remote server
 *
 * @author frank.chen021@outlook.com
 * @date 2021/6/28 10:41 上午
 */
public class BrpcAgentController implements IAgentController {
    private static final ILogAdaptor LOG = LoggerFactory.getLogger(BrpcAgentController.class);

    private final ClientChannel channel;
    private ISettingFetcher fetcher;

    public BrpcAgentController(AgentControllerConfig config) {
        List<EndPoint> endpoints = Stream.of(config.getServers().split(",")).map(hostAndPort -> {
            String[] parts = hostAndPort.split(":");
            return new EndPoint(parts[0], Integer.parseInt(parts[1]));
        }).collect(Collectors.toList());

        AppInstance appInstance = AgentContext.getInstance().getAppInstance();
        channel = ClientChannelBuilder.builder()
                                      .endpointProvider(new RoundRobinEndPointProvider(endpoints))
                                      .workerThreads(2)
                                      .applicationName(appInstance.getQualifiedAppName())
                                      .maxRetry(3)
                                      .retryInterval(Duration.ofSeconds(2))
                                      .build();

        if (appInstance.getPort() > 0) {
            channel.setAppId(appInstance.getHostAndPort());
        }

        // Update appId once the port is configured
        appInstance.addListener((port) -> channel.setAppId(AgentContext.getInstance().getAppInstance().getHostAndPort()));
    }

    @Override
    public Map<String, String> fetch(String appName, String env, long lastModifiedSince) {
        if (fetcher == null) {
            try {
                fetcher = channel.getRemoteService(ISettingFetcher.class);
            } catch (ServiceInvocationException e) {
                LOG.warn("Unable to get remote ISettingFetcher service: {}", e.getMessage());
                return null;
            }
        }

        AppInstance appInstance = AgentContext.getInstance().getAppInstance();
        BrpcMessageHeader header = BrpcMessageHeader.newBuilder()
                                                    .setAppName(appInstance.getAppName())
                                                    .setEnv(appInstance.getEnv())
                                                    .setInstanceName(appInstance.getHostAndPort())
                                                    .setHostIp(appInstance.getHostIp())
                                                    .setPort(appInstance.getPort())
                                                    .setAppType(ApplicationType.JAVA)
                                                    .build();
        try {
            return fetcher.fetch(header, lastModifiedSince);
        } catch (CallerSideException e) {
            //suppress client exception
            LOG.error("Failed to fetch settings: {}", e.getMessage());
            return null;
        } catch (CalleeSideException e) {
            //suppress stack trace since this exception occurs at server side
            LOG.error("Failed to fetch settings due to server side exception:\n {}", e.getMessage());
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
