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
import feign.codec.Decoder;
import feign.codec.Encoder;
import org.bithon.component.commons.concurrency.NamedThreadFactory;
import org.bithon.server.discovery.client.nacos.NacosDiscoveryClient;
import org.bithon.server.discovery.declaration.DiscoverableService;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
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
            applicationContext.getBean(NacosAutoServiceRegistration.class);
            return new NacosDiscoveryClient(applicationContext.getBean(NacosServiceDiscovery.class),
                                            applicationContext.getBean(NacosDiscoveryProperties.class));
        } catch (NoSuchBeanDefinitionException ignored) {
        }
        return null;
    }

    public <T> T create(Class<T> serviceDefinitionClazz) {
        DiscoverableService serviceDeclaration = serviceDefinitionClazz.getAnnotation(DiscoverableService.class);
        if (serviceDeclaration == null) {
        }

        // TODO: CHECK all methods must return type of Collection

        //noinspection unchecked
        return (T) Proxy.newProxyInstance(serviceDefinitionClazz.getClassLoader(),
                                          new Class<?>[]{serviceDefinitionClazz},
                                          new ServiceBroadcastInvocationHandler<>(serviceDefinitionClazz,
                                                                                  serviceDeclaration.name()));
    }

    private class ServiceBroadcastInvocationHandler<T> implements InvocationHandler {
        private final Class<T> type;
        private final String serviceName;

        private ServiceBroadcastInvocationHandler(Class<T> type,
                                                  String name) {
            this.type = type;
            this.serviceName = name;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {

            List<Future<Collection>> futures = new ArrayList<>(4);

            // Invoke remote service on each instance
            List<IDiscoveryClient.HostAndPort> hostAndPortList = serviceDiscoveryClient.getInstanceList(serviceName);
            for (IDiscoveryClient.HostAndPort hostAndPort : hostAndPortList) {
                futures.add(executorService.submit(new RemoteServiceCaller<>(type, hostAndPort, method, args)));
            }

            // Wait and merge the result together
            List results = new ArrayList();
            for (Future<Collection> future : futures) {
                try {
                    results.addAll(future.get());
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } catch (ExecutionException e) {
                    throw new RuntimeException(e);
                }
            }

            return results;
        }
    }

    private class RemoteServiceCaller<T> implements Callable<Collection> {

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
        public Collection call() {
            Object proxyObject = Feign.builder()
                                      .contract(applicationContext.getBean(Contract.class))
                                      .encoder(applicationContext.getBean(Encoder.class))
                                      .decoder(applicationContext.getBean(Decoder.class))
                                      .target(type, "http://" + hostAndPort.getHost() + ":" + hostAndPort.getPort());

            InvocationHandler handler = Proxy.getInvocationHandler(proxyObject);
            try {
                // The remote service must return type of Collection
                return (Collection) handler.invoke(proxyObject, method, args);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
    }
}
