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

package org.bithon.server.discovery.client;

import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import com.alibaba.cloud.nacos.discovery.NacosServiceDiscovery;
import com.alibaba.cloud.nacos.registry.NacosAutoServiceRegistration;
import feign.Contract;
import feign.Feign;
import feign.FeignException;
import feign.codec.Decoder;
import feign.codec.Encoder;
import org.bithon.component.commons.concurrency.NamedThreadFactory;
import org.bithon.component.commons.exception.HttpMappableException;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.discovery.client.nacos.NacosDiscoveryClient;
import org.bithon.server.discovery.declaration.DiscoverableService;
import org.bithon.server.discovery.declaration.ServiceResponse;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.http.HttpStatus;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author Frank Chen
 * @date 23/2/23 12:47 am
 */
public class ServiceBroadcastInvoker implements ApplicationContextAware {
    private IDiscoveryClient serviceDiscoveryClient;
    private ApplicationContext applicationContext;

    private final ExecutorService executorService = Executors.newCachedThreadPool(new NamedThreadFactory("service-invoker", true));

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
        this.serviceDiscoveryClient = createDiscoveryClient(applicationContext);
    }

    private IDiscoveryClient createDiscoveryClient(ApplicationContext applicationContext) {
        try {
            // Try to create a Nacos client first
            applicationContext.getBean(NacosAutoServiceRegistration.class);
            return new NacosDiscoveryClient(applicationContext.getBean(NacosServiceDiscovery.class),
                                            applicationContext.getBean(NacosDiscoveryProperties.class));
        } catch (NoSuchBeanDefinitionException ignored) {
        }

        // Service Discovery is not enabled
        return null;
    }

    /**
     * Create invoker for given interface.
     * @param serviceDeclaration An interface which MUST be annotated by {@link DiscoverableService}
     */
    public <T> T create(Class<T> serviceDeclaration) {
        DiscoverableService metadata = serviceDeclaration.getAnnotation(DiscoverableService.class);
        if (metadata == null) {
            throw new RuntimeException(StringUtils.format("Given class [%s] is not marked by annotation [%s].",
                                                          serviceDeclaration.getName(),
                                                          DiscoverableService.class.getSimpleName()));
        }

        //noinspection unchecked
        return (T) Proxy.newProxyInstance(serviceDeclaration.getClassLoader(),
                                          new Class<?>[]{serviceDeclaration},
                                          new ServiceBroadcastInvocationHandler<>(serviceDeclaration,
                                                                                  metadata.name()));
    }

    private class ServiceBroadcastInvocationHandler<T> implements InvocationHandler {
        private final Class<T> type;
        private final String serviceName;

        private ServiceBroadcastInvocationHandler(Class<T> type,
                                                  String name) {
            this.type = type;
            this.serviceName = name;
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            if (serviceDiscoveryClient == null) {
                throw new HttpMappableException(HttpStatus.SERVICE_UNAVAILABLE.value(),
                                                "This API is unavailable because Service Discovery is not configured.");
            }

            // Get all instances first
            List<IDiscoveryClient.HostAndPort> instanceList = serviceDiscoveryClient.getInstanceList(serviceName);

            //
            // Invoke remote service on each instance
            //
            List<Future<ServiceResponse<?>>> futures = new ArrayList<>(instanceList.size());
            for (IDiscoveryClient.HostAndPort hostAndPort : instanceList) {
                futures.add(executorService.submit(new RemoteServiceCaller<>(type, hostAndPort, method, args)));
            }

            // Since the deserialized rows object might be unmodifiable, we always create a new array to hold the final result
            List mergedRows = new ArrayList();

            //
            // Merge the result together
            //
            for (Future<ServiceResponse<?>> future : futures) {
                try {
                    ServiceResponse<?> response = future.get();
                    if (response.getError() != null) {
                        return response;
                    }

                    // Merge response
                    mergedRows.addAll(response.getRows());
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
            }

            return ServiceResponse.success(mergedRows);
        }
    }

    private class RemoteServiceCaller<T> implements Callable<ServiceResponse<?>> {

        private final Class<T> type;

        private final IDiscoveryClient.HostAndPort hostAndPort;
        private final Method method;
        private final Object[] args;

        private RemoteServiceCaller(Class<T> type, IDiscoveryClient.HostAndPort hostAndPort, Method method, Object[] args) {
            this.type = type;
            this.hostAndPort = hostAndPort;
            this.method = method;
            this.args = args;
        }

        @Override
        public ServiceResponse<?> call() {
            Object proxyObject = Feign.builder()
                                      .contract(applicationContext.getBean(Contract.class))
                                      .encoder(applicationContext.getBean(Encoder.class))
                                      .decoder(applicationContext.getBean(Decoder.class))
                                      .target(type, "http://" + hostAndPort.getHost() + ":" + hostAndPort.getPort());

            InvocationHandler handler = Proxy.getInvocationHandler(proxyObject);
            try {
                // The remote service must return type of Collection
                return (ServiceResponse<?>) handler.invoke(proxyObject, method, args);
            } catch (FeignException.NotFound e) {
                // Ignore the exception that the target service does not have data of given args
                // This might also ignore the HTTP layer 404 problem
                return ServiceResponse.EMPTY;
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
    }
}
