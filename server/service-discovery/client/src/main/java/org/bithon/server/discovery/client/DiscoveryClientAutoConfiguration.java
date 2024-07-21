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
import org.bithon.server.discovery.client.inprocess.InProcessDiscoveryClient;
import org.bithon.server.discovery.client.nacos.NacosDiscoveryClient;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignClientsConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.List;

/**
 * @author Frank Chen
 * @date 23/2/23 9:12 pm
 */
@EnableFeignClients
@Import(FeignClientsConfiguration.class)
@Configuration(proxyBeanMethods = false)
public class DiscoveryClientAutoConfiguration {

    /**
     * Define as a bean so that other services can access this executor
     */
    @Bean
    ServiceInvocationExecutor serviceInvocationExecutor() {
        return new ServiceInvocationExecutor();
    }

    @Bean
    IDiscoveryClient discoveryClient(ApplicationContext applicationContext) {
        // Create delegate lazily to avoid circular dependency
        // See: https://github.com/FrankChen021/bithon/issues/838
        return new IDiscoveryClient() {
            private volatile IDiscoveryClient delegate;

            @Override
            public List<DiscoveredServiceInstance> getInstanceList(String serviceName) {
                if (delegate == null) {
                    synchronized (this) {
                        if (delegate == null) {
                            delegate = createDelegate();
                        }
                    }
                    this.delegate = createDelegate();
                }

                return this.delegate.getInstanceList(serviceName);
            }

            private IDiscoveryClient createDelegate() {
                try {
                    // Try to create a Nacos client first
                    applicationContext.getBean(NacosAutoServiceRegistration.class);
                    return new NacosDiscoveryClient(applicationContext.getBean(NacosServiceDiscovery.class),
                                                    applicationContext.getBean(NacosDiscoveryProperties.class));
                } catch (NoSuchBeanDefinitionException ignored) {
                }

                // Service Discovery is not enabled, use Local
                return new InProcessDiscoveryClient(applicationContext);
            }
        };
    }

    @Bean
    DiscoveredServiceInvoker serviceBroadcastInvoker(IDiscoveryClient discoveryClient, ServiceInvocationExecutor executor) {
        return new DiscoveredServiceInvoker(discoveryClient, executor);
    }
}
