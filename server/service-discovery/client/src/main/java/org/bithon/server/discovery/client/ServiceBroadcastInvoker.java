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
import org.bithon.server.discovery.client.nacos.NacosDiscoveryClient;
import org.bithon.server.discovery.declaration.DiscoverableService;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * @author Frank Chen
 * @date 23/2/23 12:47 am
 */
public class ServiceBroadcastInvoker implements ApplicationContextAware {
    private IDiscoveryClient serviceDiscoveryClient;

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
        serviceDiscoveryClient.getInstanceList(serviceDeclaration.name());
        return null;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.serviceDiscoveryClient = createDiscoveryClient(applicationContext);
    }
}
