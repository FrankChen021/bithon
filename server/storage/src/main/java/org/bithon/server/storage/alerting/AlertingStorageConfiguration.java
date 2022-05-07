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

package org.bithon.server.storage.alerting;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.storage.InvalidConfigurationException;
import org.bithon.server.storage.common.TTLConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/**
 * @author Frank Chen
 * @date 20/3/22 5:06 PM
 */
public class AlertingStorageConfiguration {

    @Bean
    public IEvaluationLogStorage evaluationLogStorage(EvaluationLogConfig properties,
                                                      ObjectMapper objectMapper) throws IOException {
        InvalidConfigurationException.throwIf(StringUtils.isEmpty(properties.getType()),
                                              "[%s] can't be blank",
                                              properties.getClass(),
                                              "type");

        String jsonType = StringUtils.format("{\"type\":\"%s\"}", properties.getType());
        IEvaluationLogStorage storage = objectMapper.readValue(jsonType, IEvaluationLogStorage.class);
        storage.initialize();
        return storage;
    }

    @Bean
    public IAlertObjectStorage alertObjectStorage(ObjectMapper objectMapper,
                                                  @Value("${bithon.alerting.configuration.type}") String storageType) throws IOException {
        String jsonType = StringUtils.format("{\"type\":\"%s\"}", storageType);
        IAlertObjectStorage storage = objectMapper.readValue(jsonType, IAlertObjectStorage.class);
        storage.initialize();
        return storage;
    }

    @Bean
    public IAlertRecordStorage alertRecordStorage(ObjectMapper objectMapper,
                                                  AlertRecordConfig recordConfig) throws IOException {
        String jsonType = StringUtils.format("{\"type\":\"%s\"}", recordConfig.getType());
        IAlertRecordStorage storage = objectMapper.readValue(jsonType, IAlertRecordStorage.class);
        storage.initialize();
        return storage;
    }

    @Bean
    public AlertStorageCleaner alertStorageCleaner(IEvaluationLogStorage evaluationLogStorage,
                                                   EvaluationLogConfig logConfig,
                                                   IAlertRecordStorage recordStorage,
                                                   AlertRecordConfig recordConfig) {
        return new AlertStorageCleaner(evaluationLogStorage, logConfig, recordStorage, recordConfig);
    }

    @Data
    @Configuration
    @ConfigurationProperties(prefix = "bithon.alerting.logger")
    public static class EvaluationLogConfig {
        private String type;
        private TTLConfig ttl;
    }

    @Data
    @Configuration
    @ConfigurationProperties(prefix = "bithon.alerting.configuration")
    public static class AlertRecordConfig {
        private String type;
        private TTLConfig ttl;
    }
}
