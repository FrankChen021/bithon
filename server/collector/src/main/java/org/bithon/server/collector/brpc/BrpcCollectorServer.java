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

package org.bithon.server.collector.brpc;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bithon.component.brpc.channel.BrpcServer;
import org.bithon.component.brpc.channel.BrpcServerBuilder;
import org.bithon.component.commons.concurrency.NamedThreadFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/10 4:27 下午
 */
@Slf4j
@Component
public class BrpcCollectorServer {

    /**
     * key - port
     * val - services running on this port
     */
    private final Map<Integer, ServiceGroup> serviceGroups = new HashMap<>();

    static {
        // Make sure the underlying netty use JDK direct memory region so that the memory can be tracked
        System.setProperty("org.bithon.shaded.io.netty.maxDirectMemory", "0");
    }

    public synchronized ServiceGroup addService(String group, Object implementation, int port) {
        ServiceGroup serviceGroup = serviceGroups.computeIfAbsent(port, k -> new ServiceGroup());
        serviceGroup.getServices().put(group, implementation);

        if (serviceGroup.brpcServer == null) {
            // Create a server with the first service name as the server id
            serviceGroup.brpcServer = BrpcServerBuilder.builder()
                                                       .serverId(group)
                                                       .executor(new ThreadPoolExecutor(1,
                                                                                        Runtime.getRuntime().availableProcessors(),
                                                                                        3,
                                                                                        TimeUnit.MINUTES,
                                                                                        new LinkedBlockingQueue<>(1024),
                                                                                        NamedThreadFactory.daemonThreadFactory("brpc-executor-" + group),
                                                                                        new ThreadPoolExecutor.CallerRunsPolicy()))
                                                       .build();
            serviceGroup.start(port);
            log.info("Started Brpc services [{}] at port {}",
                     String.join(",", serviceGroup.services.keySet()),
                     port);
        } else {
            serviceGroup.brpcServer.bindService(implementation);
        }
        return serviceGroup;
    }

    @Getter
    public static class ServiceGroup {
        /**
         * key - service name
         * val - service implementation
         */
        private final Map<String, Object> services = new HashMap<>();
        private BrpcServer brpcServer;
        private int port;

        public void start(Integer port) {
            for (Object implementation : services.values()) {
                brpcServer.bindService(implementation);
            }
            brpcServer.start(port);
            this.port = port;
        }

        public synchronized void stop(String service) {
            if (this.services.remove(service) != null && this.services.isEmpty()) {
                // close channel first
                log.info("Closing channel hosting on {}", port);
                try {
                    brpcServer.close();
                } catch (Exception ignored) {
                }

                // close collector processing
                for (Map.Entry<String, Object> entry : services.entrySet()) {
                    String name = entry.getKey();
                    Object implementation = entry.getValue();
                    log.info("Closing collector services: {}", name);

                    if (implementation instanceof AutoCloseable) {
                        try {
                            ((AutoCloseable) implementation).close();
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        }
    }
}
