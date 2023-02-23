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
import org.bithon.server.discovery.client.nacos.NacosDiscoveryClient;
import org.bithon.server.discovery.declaration.DiscoverableService;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

/**
 * @author Frank Chen
 * @date 23/2/23 12:47 am
 */
public class ServiceBroadcastInvoker implements ApplicationContextAware {
    private IDiscoveryClient serviceDiscoveryClient;
    private ApplicationContext applicationContext;

    private IDiscoveryClient createDiscoveryClient(ApplicationContext applicationContext) {
        if (applicationContext.getBean(NacosAutoServiceRegistration.class) != null) {
            return new NacosDiscoveryClient(applicationContext.getBean(NacosServiceDiscovery.class),
                                            applicationContext.getBean(NacosDiscoveryProperties.class));
        }
        return null;
    }

    public <T> T create(Class<T> clazz) {
        DiscoverableService serviceDeclaration = clazz.getAnnotation(DiscoverableService.class);
        if (serviceDeclaration == null) {

        }

        //noinspection unchecked
        return (T) Proxy.newProxyInstance(clazz.getClassLoader(),
                                          new Class<?>[]{clazz},
                                          new ServiceBroadcastInvocationHandler(clazz,
                                                                                serviceDiscoveryClient,
                                                                                serviceDeclaration.name(),
                                                                                applicationContext));
    }

    private static class ServiceBroadcastInvocationHandler implements InvocationHandler {
        private final Class type;
        private final IDiscoveryClient serviceDiscoveryClient;
        private final String serviceName;
        private final ApplicationContext applicationContext;

        private ServiceBroadcastInvocationHandler(Class type,
                                                  IDiscoveryClient serviceDiscoveryClient,
                                                  String name,
                                                  ApplicationContext applicationContext) {
            this.type = type;
            this.serviceDiscoveryClient = serviceDiscoveryClient;
            this.serviceName = name;
            this.applicationContext = applicationContext;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {

            List<IDiscoveryClient.HostAndPort> hostAndPortList = serviceDiscoveryClient.getInstanceList(serviceName);

            for (IDiscoveryClient.HostAndPort hostAndPort : hostAndPortList) {
                Object proxyObject = Feign.builder()
                                          .contract(applicationContext.getBean(Contract.class))
                                          .encoder(applicationContext.getBean(Encoder.class))
                                          .decoder(applicationContext.getBean(Decoder.class))
                                          .target(type, "http://" + hostAndPort.getHost() + ":" + hostAndPort.getPort());

                InvocationHandler handler = Proxy.getInvocationHandler(proxyObject);
                try {
                    Object object = handler.invoke(proxyObject, method, args);

                    // TODO: 1. MERGE the result together
                    // TODO: 2. use thread pool to execute invocation for each instance
                } catch (Throwable e) {
                }

                // Merge the output
            }

            return null;
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
        this.serviceDiscoveryClient = createDiscoveryClient(applicationContext);
    }
}
