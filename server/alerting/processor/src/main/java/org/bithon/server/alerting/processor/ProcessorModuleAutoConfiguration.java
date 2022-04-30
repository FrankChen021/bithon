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

package org.bithon.server.alerting.processor;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.alerting.common.AlertingModule;
import org.bithon.server.alerting.processor.storage.AlertStateLocalMemoryStorage;
import org.bithon.server.storage.alerting.AlertingStorageConfiguration;
import org.bithon.server.storage.alerting.IAlertStateStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/**
 * @author Frank Chen
 * @date 12/11/21 6:18 pm
 */
@Configuration
@ConditionalOnProperty(prefix = "bithon.alerting.processor", name = "enabled", havingValue = "true")
@ImportAutoConfiguration(value = {AlertingStorageConfiguration.class})
public class ProcessorModuleAutoConfiguration {

    @Bean
    AlertingModule processorModule() {
        return new AlertingModule();
    }


    @Bean
    public IAlertStateStorage alertStateStorage(ObjectMapper objectMapper,
                                                @Value("${bithon.alerting.state.type}") String storageType) throws IOException {
        String jsonType = StringUtils.format("{\"type\":\"%s\"}", storageType);
        return objectMapper.readValue(jsonType, IAlertStateStorage.class);
    }

    @Bean
    public Module alertingProcessorModule() {
        return new Module() {
            @Override
            public String getModuleName() {
                return "alerting-processor";
            }

            @Override
            public Version version() {
                return Version.unknownVersion();
            }

            @Override
            public void setupModule(SetupContext context) {
                context.registerSubtypes(AlertStateLocalMemoryStorage.class);
            }
        };
    }
}
