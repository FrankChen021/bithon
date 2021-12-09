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

package org.bithon.server.collector.source.brpc;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bithon.agent.rpc.brpc.event.IEventCollector;
import org.bithon.agent.rpc.brpc.metrics.IMetricCollector;
import org.bithon.agent.rpc.brpc.setting.ISettingFetcher;
import org.bithon.agent.rpc.brpc.tracing.ITraceCollector;
import org.bithon.component.brpc.channel.ServerChannel;
import org.bithon.server.collector.cmd.api.CommandService;
import org.bithon.server.collector.setting.AgentSettingService;
import org.bithon.server.collector.setting.BrpcSettingFetcher;
import org.bithon.server.collector.sink.IMessageSink;
import org.springframework.beans.BeansException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/10 4:27 下午
 */
@Slf4j
@Component
@ConditionalOnProperty(value = "collector-brpc.enabled", havingValue = "true", matchIfMissing = false)
public class BrpcCollectorStarter implements SmartLifecycle, ApplicationContextAware {

    private final List<ServerChannel> servers = new ArrayList<>();
    private ApplicationContext applicationContext;
    private boolean isRunning;

    @Getter
    @AllArgsConstructor
    static class ServiceImpl {
        private final Class<?> clazz;
        private final Object impl;
    }

    @Getter
    static class ServiceGroup {
        private final List<ServiceImpl> services = new ArrayList<>();
        private boolean isCtrl;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void start() {
        BrpcCollectorConfig config = applicationContext.getBean(BrpcCollectorConfig.class);
        Map<Integer, ServiceGroup> serviceGroups = new HashMap<>();

        //
        // group services by their listening ports
        //
        for (Map.Entry<String, Integer> entry : config.getPort().entrySet()) {
            String service = entry.getKey();
            Integer port = entry.getValue();

            boolean isCtrl = false;
            Class<?> clazz = null;
            Object serviceProvider = null;
            switch (service) {
                case "metric":
                    clazz = IMetricCollector.class;
                    serviceProvider = new BrpcMetricCollector(applicationContext.getBean("schemaMetricSink", IMessageSink.class),
                                                              applicationContext.getBean("metricSink", IMessageSink.class));
                    break;

                case "event":
                    clazz = IEventCollector.class;
                    serviceProvider = new BrpcEventCollector(applicationContext.getBean("eventSink",
                                                                                        IMessageSink.class));
                    break;

                case "tracing":
                    clazz = ITraceCollector.class;
                    serviceProvider = new BrpcTraceCollector(applicationContext.getBean("traceSink",
                                                                                        IMessageSink.class));
                    break;

                case "ctrl":
                    isCtrl = true;
                    clazz = ISettingFetcher.class;
                    serviceProvider = new BrpcSettingFetcher(applicationContext.getBean(AgentSettingService.class));
                    break;

                default:
                    break;
            }
            if (serviceProvider != null) {
                ServiceGroup serviceGroup = serviceGroups.computeIfAbsent(port, key -> new ServiceGroup());
                serviceGroup.isCtrl = isCtrl;
                serviceGroup.getServices().add(new ServiceImpl(clazz, serviceProvider));
            }
        }

        serviceGroups.forEach((port, serviceGroup) -> {
            ServerChannel channel = new ServerChannel();
            if (serviceGroup.isCtrl) {
                applicationContext.getBean(CommandService.class).setServerChannel(channel);
            }
            serviceGroup.getServices().forEach((service) -> channel.bindService(service.getImpl()));
            channel.start(port);
        });

        isRunning = true;
    }

    @Override
    public void stop() {
        servers.forEach(ServerChannel::close);
        isRunning = false;
    }

    @Override
    public boolean isRunning() {
        return isRunning;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
