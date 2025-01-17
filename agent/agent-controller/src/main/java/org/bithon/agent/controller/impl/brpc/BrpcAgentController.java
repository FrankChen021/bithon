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

package org.bithon.agent.controller.impl.brpc;

import org.bithon.agent.AgentBuildVersion;
import org.bithon.agent.controller.AgentControllerConfig;
import org.bithon.agent.controller.IAgentController;
import org.bithon.agent.observability.context.AppInstance;
import org.bithon.agent.rpc.brpc.ApplicationType;
import org.bithon.agent.rpc.brpc.BrpcMessageHeader;
import org.bithon.agent.rpc.brpc.setting.ISettingFetcher;
import org.bithon.component.brpc.channel.BrpcClient;
import org.bithon.component.brpc.channel.BrpcClientBuilder;
import org.bithon.component.brpc.endpoint.EndPoint;
import org.bithon.component.brpc.endpoint.RoundRobinEndPointProvider;
import org.bithon.component.brpc.exception.CalleeSideException;
import org.bithon.component.brpc.exception.CallerSideException;
import org.bithon.component.brpc.exception.ServiceInvocationException;
import org.bithon.component.brpc.message.Headers;
import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A controller that accepts and serves commands from a remote server
 *
 * @author frank.chen021@outlook.com
 * @date 2021/6/28 10:41 上午
 */
public class BrpcAgentController implements IAgentController {
    private static final ILogAdaptor LOG = LoggerFactory.getLogger(BrpcAgentController.class);

    private final BrpcClient brpcClient;
    private ISettingFetcher fetcher;
    private Runnable refreshListener;

    public BrpcAgentController(AgentControllerConfig config) {
        List<EndPoint> endpoints = Stream.of(config.getServers().split(",")).map(hostAndPort -> {
            String[] parts = hostAndPort.split(":");
            return new EndPoint(parts[0], Integer.parseInt(parts[1]));
        }).collect(Collectors.toList());

        AppInstance appInstance = AppInstance.getInstance();
        brpcClient = BrpcClientBuilder.builder()
                                      .applicationName(appInstance.getQualifiedName())
                                      .clientId("ctrl")
                                      .server(new RoundRobinEndPointProvider(endpoints))
                                      .workerThreads(2)
                                      .maxRetry(3)
                                      .retryInterval(Duration.ofSeconds(2))
                                      .connectionTimeout(config.getClient().getConnectionTimeout())
                                      .header(Headers.HEADER_VERSION, AgentBuildVersion.getString())
                                      .header(Headers.HEADER_START_TIME, String.valueOf(ManagementFactory.getRuntimeMXBean().getStartTime()))
                                      .build();

        if (appInstance.getPort() > 0) {
            // Set the default the appId
            brpcClient.setHeader(Headers.HEADER_APP_ID, appInstance.getInstanceName());
        }

        // Update appId once the port is configured,
        // so that the management API in the server side can find this agent by appId correctly
        appInstance.addListener((port) -> {
            brpcClient.setHeader(Headers.HEADER_APP_ID, AppInstance.getInstance().getInstanceName());

            if (refreshListener != null) {
                try {
                    refreshListener.run();
                } catch (Exception e) {
                    LOG.error("Failed to call refresh listener.", e);
                }
            }
        });
    }

    @Override
    public Map<String, String> getAgentConfiguration(String appName, String env, long lastModifiedSince) {
        if (fetcher == null) {
            try {
                fetcher = brpcClient.getRemoteService(ISettingFetcher.class);
            } catch (ServiceInvocationException e) {
                LOG.warn("Unable to get remote ISettingFetcher service: {}", e.getMessage());
                return null;
            }
        }

        AppInstance appInstance = AppInstance.getInstance();
        BrpcMessageHeader header = BrpcMessageHeader.newBuilder()
                                                    .setAppName(appInstance.getName())
                                                    .setEnv(appInstance.getEnv())
                                                    .setInstanceName(appInstance.getInstanceName())
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
    public void refreshListener(Runnable callback) {
        this.refreshListener = callback;
    }

    @Override
    public void attachCommands(Object... commands) {
        for (Object cmd : commands) {
            brpcClient.bindService(cmd);
        }
    }

    @Override
    public void close() {
        brpcClient.close();
    }
}
