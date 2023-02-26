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

package org.bithon.server.discovery.registration;

import com.alibaba.cloud.nacos.registry.NacosRegistrationCustomizer;
import org.bithon.server.discovery.declaration.DiscoverableService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * @author Frank Chen
 * @date 22/2/23 11:22 pm
 */
@Configuration
public class DiscoveryRegistration {

    //    @ConditionalOnBean(NacosServiceRegistry.class)
    @Bean
    public NacosRegistrationCustomizer nacosRegistrationCustomizer(ApplicationContext applicationContext) {
        // Register these services as metadata of bithon-service,
        // So that discovery clients can find these registered services
        return registration -> {
            Map<String, String> registrationServices = registration.getMetadata();

            // Find declared services
            Map<String, Object> services = applicationContext.getBeansWithAnnotation(DiscoverableService.class);
            for (Map.Entry<String, Object> entry : services.entrySet()) {
                DiscoverableService serviceDeclaration = entry.getValue().getClass().getAnnotation(DiscoverableService.class);
                registrationServices.put("bithon.service." + serviceDeclaration.name(), "true");
            }
        };
    }
}
