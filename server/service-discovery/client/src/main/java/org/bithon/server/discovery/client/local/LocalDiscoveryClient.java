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

package org.bithon.server.discovery.client.local;

import org.bithon.component.commons.exception.HttpMappableException;
import org.bithon.server.discovery.client.IDiscoveryClient;
import org.bithon.server.discovery.declaration.DiscoverableService;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * This is mainly for local development. Should not be used in production.
 *
 * @author frank.chen021@outlook.com
 * @date 2023/3/21 23:55
 */
public class LocalDiscoveryClient implements IDiscoveryClient {

    private final ApplicationContext applicationContext;

    public LocalDiscoveryClient(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public List<HostAndPort> getInstanceList(String serviceName) {
        Map<String, Object> serviceProviders = applicationContext.getBeansWithAnnotation(DiscoverableService.class);
        if (serviceProviders.isEmpty()) {
            // The ServiceProvider is not deployed with the web-server module
            throw new HttpMappableException(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                            "Can't find the service provider: The Bithon web-server is neither deployed with collector nor deployed under Alibaba Nacos.");
        }

        for (Map.Entry<String, Object> entry : serviceProviders.entrySet()) {
            Object serviceProvider = entry.getValue();

            Class<?> serviceClazz = serviceProvider.getClass();

            // Root parent must be Object in Java
            while (serviceClazz != null) {

                for (Class<?> intf : serviceClazz.getInterfaces()) {
                    DiscoverableService annotation = intf.getAnnotation(DiscoverableService.class);
                    if (annotation != null && serviceName.equals(annotation.name())) {
                        return Collections.singletonList(new HostAndPort("localhost",
                                                                         applicationContext.getEnvironment().getProperty("server.port", Integer.class)));
                    }
                }

                serviceClazz = serviceClazz.getSuperclass();
            }
        }
        return Collections.emptyList();
    }
}
