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
import cn.bithon.rpc.services.ISettingFetcher;
import cn.bithon.rpc.services.MessageHeader;
import com.sbss.bithon.agent.core.config.FetcherConfig;
import com.sbss.bithon.agent.core.context.AgentContext;
import com.sbss.bithon.agent.core.context.AppInstance;
import com.sbss.bithon.agent.core.setting.IAgentSettingFetcher;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/6/28 10:41 上午
 */
public class SettingFetcher implements IAgentSettingFetcher {
    private final ISettingFetcher fetcher;

    public SettingFetcher(FetcherConfig config) {
        List<EndPoint> endpoints = Stream.of(config.getServers().split(",")).map(hostAndPort -> {
            String[] parts = hostAndPort.split(":");
            return new EndPoint(parts[0], Integer.parseInt(parts[1]));
        }).collect(Collectors.toList());
        fetcher = new ClientChannel(new RoundRobinEndPointProvider(endpoints))
            .configureRetry(3, Duration.ofMillis(100))
            .getRemoteService(ISettingFetcher.class);
    }

    @Override
    public Map<String, String> fetch(String appName, String env, long lastModifiedSince) {
        AppInstance appInstance = AgentContext.getInstance().getAppInstance();
        MessageHeader header = MessageHeader.newBuilder()
                                            .setAppName(appInstance.getRawAppName())
                                            .setEnv(appInstance.getEnv())
                                            .setInstanceName(appInstance.getHostIp() + ":" + appInstance.getPort())
                                            .setHostIp(appInstance.getHostIp())
                                            .setPort(appInstance.getPort())
                                            .setAppType(ApplicationType.JAVA)
                                            .build();
        return fetcher.fetch(header, lastModifiedSince);
    }
}
