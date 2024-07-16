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

package org.bithon.server.discovery.client.inprocess;

import org.bithon.component.commons.exception.HttpMappableException;
import org.bithon.server.discovery.client.DiscoveredServiceInstance;
import org.bithon.server.discovery.client.IDiscoveryClient;
import org.bithon.server.discovery.declaration.DiscoverableService;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * This is mainly for local development, not recommended in production.
 * For local deployment, multiline modules are deployed in one process, we can find the service provider through {@link ApplicationContext}.
 *
 * @author frank.chen021@outlook.com
 * @date 2023/3/21 23:55
 */
public class InProcessDiscoveryClient implements IDiscoveryClient {

    private final ApplicationContext applicationContext;

    public InProcessDiscoveryClient(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public List<DiscoveredServiceInstance> getInstanceList(String serviceName) {
        // Find the service implementations in the current running process
        Map<String, Object> serviceProviders = applicationContext.getBeansWithAnnotation(DiscoverableService.class);
        if (serviceProviders.isEmpty()) {
            // The ServiceProvider is not deployed with the web-server module
            throw new HttpMappableException(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                            "Can't find the service provider: The Bithon web-server is neither deployed with collector nor deployed under any service discovery service such as Alibaba Nacos.");
        }

        // Find given service by name
        for (Map.Entry<String, Object> serviceProviderEntry : serviceProviders.entrySet()) {
            Class<?> serviceProviderClazz = serviceProviderEntry.getValue().getClass();

            // Find the 'DiscoverableService' annotation declaration on the class and its superclasses
            while (serviceProviderClazz != null) {

                for (Class<?> serviceInterface : serviceProviderClazz.getInterfaces()) {
                    DiscoverableService annotation = serviceInterface.getAnnotation(DiscoverableService.class);
                    if (annotation != null && serviceName.equals(annotation.name())) {
                        // Found. Return current application instance
                        return Collections.singletonList(new DiscoveredServiceInstance("localhost",
                                                                                       applicationContext.getEnvironment().getProperty("server.port", Integer.class)));
                    }
                }

                serviceProviderClazz = serviceProviderClazz.getSuperclass();
            }
        }

        throw new HttpMappableException(HttpStatus.SERVICE_UNAVAILABLE.value(), "Not found any instance of service [%s]", serviceName);
    }
}
