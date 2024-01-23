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
import org.bithon.component.brpc.channel.BrpcServer;
import org.bithon.server.collector.cmd.service.AgentServer;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.SmartLifecycle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/10 4:27 下午
 */
@Slf4j
//@Component
//@ConditionalOnProperty(value = "collector-brpc.enabled", havingValue = "true", matchIfMissing = false)
public class BrpcCollectorStarter implements SmartLifecycle, ApplicationContextAware {

    private final Map<Integer, ServiceGroup> serviceGroups = new HashMap<>();
    private ApplicationContext applicationContext;
    private boolean isRunning;

    static {
        // Make sure the underlying netty use JDK direct memory region so that the memory can be tracked
        System.setProperty("org.bithon.shaded.io.netty.maxDirectMemory", "0");
    }

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
                    break;

                case "event":
                    break;

                case "ctrl":
                    isCtrl = true;
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
            // Create a server with the first service name as the server id
            BrpcServer brpcServer = new BrpcServer(serviceGroup.services.get(0).name);
            if (serviceGroup.isCtrl) {
                applicationContext.getBean(AgentServer.class).setBrpcServer(brpcServer);
            }
            serviceGroup.brpcServer = brpcServer;
            serviceGroup.start(port);

            log.info("Started Brpc services [{}] at port {}",
                     serviceGroup.services.stream().map((s) -> s.name).collect(Collectors.joining(",")),
                     port);
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
        private BrpcServer brpcServer;
        private int port;

        public void start(Integer port) {
            for (ServiceProvider service : services) {
                brpcServer.bindService(service.getImplementation());
            }
            brpcServer.start(port);
            this.port = port;
        }

        public void addService(String type, Class<?> serviceDefinition, Object serviceImplementation) {
            this.services.add(new ServiceProvider(type, serviceDefinition, serviceImplementation));
        }

        public void close() {
            // close channel first
            log.info("Closing channel hosting on {}", port);
            try {
                brpcServer.close();
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
