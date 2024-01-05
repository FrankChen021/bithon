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

package org.bithon.server.webapp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.storage.InvalidConfigurationException;
import org.bithon.server.storage.common.StorageConfig;
import org.bithon.server.storage.common.provider.StorageProviderManager;
import org.bithon.server.storage.web.IDashboardStorage;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;

/**
 * @author Frank Chen
 * @date 19/8/22 2:21 pm
 */
@Configuration
@Conditional(value = WebAppModuleEnabler.class)
public class WebAppAutoConfiguration {

    @Configuration
    @ConfigurationProperties(prefix = "bithon.storage.web")
    public static class WebAppStorageConfig extends StorageConfig {
    }

    @Bean
    public IDashboardStorage dashboardStorage(ObjectMapper om,
                                              StorageProviderManager storageProviderManager,
                                              WebAppStorageConfig storageConfig) throws IOException {
        String providerName = StringUtils.isEmpty(storageConfig.getProvider()) ? storageConfig.getType() : storageConfig.getProvider();
        InvalidConfigurationException.throwIf(!StringUtils.hasText(providerName),
                                              "[%s] can't be blank",
                                              storageConfig.getClass(),
                                              "provider");

        // create storage
        IDashboardStorage storage = storageProviderManager.createStorage(providerName, IDashboardStorage.class);
        storage.initialize();

        // load or update schemas
        try {
            Resource[] resources = new PathMatchingResourcePatternResolver()
                .getResources("classpath:/dashboard/*.json");
            for (Resource resource : resources) {

                JsonNode dashboard = om.readTree(resource.getInputStream());
                JsonNode nameNode = dashboard.get("name");
                if (nameNode == null) {
                    throw new RuntimeException(StringUtils.format("dashboard [%s] miss the name property", resource.getFilename()));
                }

                String name = nameNode.asText();
                if (StringUtils.isEmpty(name)) {
                    throw new RuntimeException(StringUtils.format("dashboard [%s] has empty name property", resource.getFilename()));
                }

                // deserialize and then serialize again to compact the json string
                String payload = om.writeValueAsString(dashboard);

                storage.putIfNotExist(nameNode.asText(), payload);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return storage;
    }
}
