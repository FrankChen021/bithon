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
import org.bithon.server.collector.cmd.service.CommandService;
import org.bithon.server.collector.config.AgentConfigurationService;
import org.bithon.server.collector.config.BrpcSettingFetcher;
import org.bithon.server.sink.common.service.UriNormalizer;
import org.bithon.server.sink.event.IEventMessageSink;
import org.bithon.server.sink.metrics.IMetricMessageSink;
import org.bithon.server.sink.tracing.ITraceMessageSink;
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
            String type = entry.getKey();
            Integer port = entry.getValue();

            boolean isCtrl = false;
            Class<?> serviceDefinition = null;
            Object serviceImplementation = null;
            switch (type) {
                case "metric":
                    serviceDefinition = IMetricCollector.class;
                    serviceImplementation = new BrpcMetricCollector(applicationContext.getBean(IMetricMessageSink.class));
                    break;

                case "event":
                    serviceDefinition = IEventCollector.class;
                    serviceImplementation = new BrpcEventCollector(applicationContext.getBean(IEventMessageSink.class));
                    break;

                case "tracing":
                    serviceDefinition = ITraceCollector.class;
                    serviceImplementation = new BrpcTraceCollector(applicationContext.getBean("trace-sink-collector", ITraceMessageSink.class),
                                                                   applicationContext.getBean(UriNormalizer.class));
                    break;

                case "ctrl":
                    isCtrl = true;
                    serviceDefinition = ISettingFetcher.class;
                    serviceImplementation = new BrpcSettingFetcher(applicationContext.getBean(AgentConfigurationService.class));
                    break;

                default:
                    break;
            }
            if (serviceImplementation != null) {
                ServiceGroup serviceGroup = serviceGroups.computeIfAbsent(port, key -> new ServiceGroup());
                serviceGroup.isCtrl = isCtrl;
                serviceGroup.addService(type, serviceDefinition, serviceImplementation);
            }
        }

        serviceGroups.forEach((port, serviceGroup) -> {
            ServerChannel channel = new ServerChannel();
            if (serviceGroup.isCtrl) {
                applicationContext.getBean(CommandService.class).setServerChannel(channel);
            }
            serviceGroup.channel = channel;
            serviceGroup.start(port);
        });

        isRunning = true;
    }

    @Override
    public void stop() {
        log.info("Shutdown Brpc collector...");
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
        private int port;

        public void start(Integer port) {
            for (ServiceProvider service : services) {
                channel.bindService(service.getImplementation());
            }
            channel.start(port);
            this.port = port;
        }

        public void addService(String type, Class<?> serviceDefinition, Object serviceImplementation) {
            this.services.add(new ServiceProvider(type, serviceDefinition, serviceImplementation));
        }

        public void close() {
            // close channel first
            log.info("Closing channel hosting on {}", port);
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
