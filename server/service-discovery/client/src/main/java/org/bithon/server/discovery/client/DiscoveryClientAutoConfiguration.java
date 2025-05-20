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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.component.commons.utils.SupplierUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignClientsConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.function.Supplier;

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
    DiscoveredServiceInvoker serviceInvoker(IDiscoveryClient discoveryClient, ServiceInvocationExecutor executor) {
        return new DiscoveredServiceInvoker(discoveryClient, executor);
    }

    @Bean
    IDiscoveryClient discoveryClient(@Value("${bithon.discovery.type:inprocess}") String discoveryType,
                                     ObjectMapper objectMapper) {
        // Create delegate lazily to avoid circular dependency
        // See: https://github.com/FrankChen021/bithon/issues/838
        return new IDiscoveryClient() {
            private final Supplier<IDiscoveryClient> delegate = SupplierUtils.cachedWithLock(() -> {
                String json = StringUtils.format("{\"type\":\"%s\"}", discoveryType);
                try {
                    return objectMapper.readValue(json, IDiscoveryClient.class);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException("Failed to create discovery client for " + discoveryType, e);
                }
            });

            @Override
            public List<DiscoveredServiceInstance> getInstanceList() {
                return delegate.get()
                               .getInstanceList();
            }

            @Override
            public List<DiscoveredServiceInstance> getInstanceList(String serviceName) {
                return delegate.get()
                               .getInstanceList(serviceName);
            }
        };
    }
}
