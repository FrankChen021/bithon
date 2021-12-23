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

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.bithon.server.event.sink.IEventMessageSink;
import org.bithon.server.metric.sink.IMessageSink;
import org.bithon.server.metric.sink.IMetricMessageSink;
import org.bithon.server.tracing.sink.ITraceMessageSink;
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

    private final Map<Integer, ServiceGroup> serviceGroups = new HashMap<>();
    private ApplicationContext applicationContext;
    private boolean isRunning;

    @SuppressWarnings("unchecked")
    @Override
    public void start() {
        BrpcCollectorConfig config = applicationContext.getBean(BrpcCollectorConfig.class);

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
                                                              applicationContext.getBean(IMetricMessageSink.class));
                    break;

                case "event":
                    clazz = IEventCollector.class;
                    serviceProvider = new BrpcEventCollector(applicationContext.getBean(IEventMessageSink.class),
                                                             applicationContext.getBean(ObjectMapper.class));
                    break;

                case "tracing":
                    clazz = ITraceCollector.class;
                    serviceProvider = new BrpcTraceCollector(applicationContext.getBean(ITraceMessageSink.class));
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
                serviceGroup.getServices().add(new ServiceProvider(service, clazz, serviceProvider));
            }
        }

        serviceGroups.forEach((port, serviceGroup) -> {
            ServerChannel channel = new ServerChannel();
            if (serviceGroup.isCtrl) {
                applicationContext.getBean(CommandService.class).setServerChannel(channel);
            }
            serviceGroup.channel = new ServerChannel();
            serviceGroup.start(port);
        });

        isRunning = true;
    }

    @Override
    public void stop() {
        serviceGroups.values().forEach((ServiceGroup::close));
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

    /**
     * Collector should be shutdown at first
     */
    @Override
    public int getPhase() {
        return DEFAULT_PHASE - 1;
    }

    @Getter
    @AllArgsConstructor
    static class ServiceProvider {
        private final String name;
        private final Class<?> service;
        private final Object implementation;
    }

    @Getter
    static class ServiceGroup {
        private final List<ServiceProvider> services = new ArrayList<>();
        private boolean isCtrl;
        private ServerChannel channel;

        public void start(Integer port) {
            for (ServiceProvider service : services) {
                channel.bindService(service.getImplementation());
            }
            channel.start(port);
        }

        public void close() {
            // close channel first
            try {
                channel.close();
            } catch (Exception ignored) {
            }

            // close collector processing
            for (ServiceProvider serviceProvider : services) {
                log.info("Closing collector services: {}", serviceProvider.name);

                if (serviceProvider.implementation instanceof AutoCloseable) {
                    try {
                        ((AutoCloseable) serviceProvider.implementation).close();
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }
}
