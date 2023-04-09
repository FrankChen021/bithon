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

package org.bithon.server.web.service.agent.sql.table;

import org.bithon.component.brpc.channel.IChannelWriter;
import org.bithon.component.brpc.invocation.ClientInvocationManager;
import org.bithon.component.brpc.message.Headers;
import org.bithon.component.commons.exception.HttpMappableException;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.discovery.client.IDiscoveryClient;
import org.bithon.server.discovery.declaration.DiscoverableService;
import org.bithon.server.discovery.declaration.ServiceResponse;
import org.bithon.shaded.io.netty.channel.Channel;
import org.springframework.http.HttpStatus;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/4/9 16:27
 */
public class AgentCommandFactory {

    private final IDiscoveryClient serviceDiscoveryClient;
    private ExecutorService executorService;

    public AgentCommandFactory(IDiscoveryClient discoveryClient) {
        this.serviceDiscoveryClient = discoveryClient;
    }

    public <T> T create(Class<?> proxyServiceDeclaration,
                        String proxy,
                        Class<T> serviceDeclaration) {
        DiscoverableService metadata = proxyServiceDeclaration.getAnnotation(DiscoverableService.class);
        if (metadata == null) {
            throw new RuntimeException(StringUtils.format("Given class [%s] is not marked by annotation [%s].",
                                                          proxyServiceDeclaration.getName(),
                                                          DiscoverableService.class.getSimpleName()));
        }

        //noinspection unchecked
        return (T) Proxy.newProxyInstance(serviceDeclaration.getClassLoader(),
                                          new Class<?>[]{serviceDeclaration},
                                          new AgentCommandBroadcastInvoker<>(metadata.name()));
    }

    private class AgentCommandBroadcastInvoker<T> implements InvocationHandler {
        private final String proxyServiceName;

        private AgentCommandBroadcastInvoker(String proxyServiceName) {
            this.proxyServiceName = proxyServiceName;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (serviceDiscoveryClient == null) {
                throw new HttpMappableException(HttpStatus.SERVICE_UNAVAILABLE.value(),
                                                "This API is unavailable because Service Discovery is not configured.");
            }

            // Get all instances first
            List<IDiscoveryClient.HostAndPort> instanceList = serviceDiscoveryClient.getInstanceList(proxyServiceName);

            //
            // Invoke remote service on each instance
            //
            List<Future<Collection<?>>> futures = new ArrayList<>(instanceList.size());
            for (IDiscoveryClient.HostAndPort hostAndPort : instanceList) {
                futures.add(executorService.submit(() -> {
                    try {
                        return ClientInvocationManager.getInstance().invoke("",
                                                                            Headers.EMPTY,
                                                                            channelWriter,
                                                                            false,
                                                                            timeout,
                                                                            method,
                                                                            args);
                    } catch (Throwable e) {
                        throw new RuntimeException(e);
                    }
                }));
            }

            // Since the deserialized rows object might be unmodifiable, we always create a new array to hold the final result
            List mergedRows = new ArrayList<>();

            //
            // Merge the result together
            //
            for (Future<Collection<?>> future : futures) {
                try {
                    Collection<?> response = future.get();

                    // Merge response
                    mergedRows.addAll(response);
                } catch (InterruptedException | ExecutionException e) {
                    if (e.getCause() instanceof HttpMappableException) {
                        throw e.getCause();
                    }
                    throw new RuntimeException(e);
                }
            }

            return mergedRows;
        }
    }

    static class FeignServiceChannel implements IChannelWriter {

        @Override
        public void connect() {
        }

        @Override
        public void disconnect() {
        }

        @Override
        public long getConnectionLifeTime() {
            return 0;
        }

        @Override
        public Channel getChannel() {
            return null;
        }

        @Override
        public void writeAndFlush(Object obj) {
            // This is a async operation
            // Can turn it into a sync?
        }
    }
}
