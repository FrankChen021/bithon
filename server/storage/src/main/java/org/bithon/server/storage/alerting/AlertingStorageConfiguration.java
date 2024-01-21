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

import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.storage.InvalidConfigurationException;
import org.bithon.server.storage.common.StorageConfig;
import org.bithon.server.storage.common.provider.StorageProviderManager;
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
    IAlertNotificationProviderStorage notificationProviderStorage(AlertStorageConfig config,
                                                                  StorageProviderManager storageProviderManager) throws IOException {
        IAlertNotificationProviderStorage storage = storageProviderManager.createStorage(config.getProvider(), IAlertNotificationProviderStorage.class);
        storage.initialize();
        return storage;
    }

    @Bean
    public IEvaluationLogStorage evaluationLogStorage(EvaluationLogConfig config,
                                                      StorageProviderManager storageProviderManager) throws IOException {
        InvalidConfigurationException.throwIf(StringUtils.isEmpty(config.getProvider()),
                                              "[%s] can't be blank",
                                              config.getClass(),
                                              "provider");

        IEvaluationLogStorage storage = storageProviderManager.createStorage(config.getProvider(), IEvaluationLogStorage.class);
        storage.initialize();
        return storage;
    }

    @Bean
    public IAlertObjectStorage alertObjectStorage(StorageProviderManager storageProviderManager,
                                                  AlertStorageConfig config) throws IOException {
        IAlertObjectStorage storage = storageProviderManager.createStorage(config.getProvider(), IAlertObjectStorage.class);
        storage.initialize();
        return storage;
    }

    @Bean
    public IAlertRecordStorage alertRecordStorage(StorageProviderManager storageProviderManager,
                                                  AlertStorageConfig config) throws IOException {
        IAlertRecordStorage storage = storageProviderManager.createStorage(config.getProvider(), IAlertRecordStorage.class);
        storage.initialize();
        return storage;
    }

    @Configuration
    @ConfigurationProperties(prefix = "bithon.storage.alerting.log")
    public static class EvaluationLogConfig extends StorageConfig {
    }

    /**
     * Module level storage
     */
    @Configuration
    @ConfigurationProperties(prefix = "bithon.storage.alerting.all")
    public static class AlertStorageConfig extends StorageConfig {
    }
}
