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

package org.bithon.server.storage;

import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.storage.common.provider.StorageProviderManager;
import org.bithon.server.storage.datasource.SchemaManager;
import org.bithon.server.storage.event.EventStorageConfig;
import org.bithon.server.storage.event.EventTableSchema;
import org.bithon.server.storage.event.IEventStorage;
import org.bithon.server.storage.meta.CacheableMetadataStorage;
import org.bithon.server.storage.meta.IMetaStorage;
import org.bithon.server.storage.meta.ISchemaStorage;
import org.bithon.server.storage.meta.MetaStorageConfig;
import org.bithon.server.storage.metrics.IMetricStorage;
import org.bithon.server.storage.metrics.MetricStorageConfig;
import org.bithon.server.storage.setting.ISettingStorage;
import org.bithon.server.storage.setting.SettingStorageConfig;
import org.bithon.server.storage.tracing.ITraceStorage;
import org.bithon.server.storage.tracing.TraceStorageConfig;
import org.bithon.server.storage.tracing.TraceTableSchema;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/30 8:34 下午
 */
@Configuration
public class StorageModuleAutoConfiguration {

    @Bean
    @ConditionalOnProperty(value = "bithon.storage.metric.enabled", havingValue = "true")
    public IMetricStorage metricStorage(MetricStorageConfig storageConfig,
                                        StorageProviderManager storageProviderManager) throws IOException {
        String providerName = StringUtils.isEmpty(storageConfig.getProvider()) ? storageConfig.getType() : storageConfig.getProvider();
        InvalidConfigurationException.throwIf(!StringUtils.hasText(providerName),
                                              "[%s] is not properly configured to enable the Metric module.",
                                              storageConfig.getClass(),
                                              "provider");

        IMetricStorage storage = storageProviderManager.createStorage(providerName, IMetricStorage.class);
        storage.initialize();
        return storage;
    }

    @Bean
    @ConditionalOnProperty(value = "bithon.storage.meta.enabled", havingValue = "true")
    public ISchemaStorage schemaStorage(MetaStorageConfig storageConfig,
                                        StorageProviderManager storageProviderManager) throws IOException {
        String providerName = StringUtils.isEmpty(storageConfig.getProvider()) ? storageConfig.getType() : storageConfig.getProvider();
        InvalidConfigurationException.throwIf(!StringUtils.hasText(providerName),
                                              "[%s] is not properly configured to enable the schema storage.",
                                              storageConfig.getClass(),
                                              "provider");

        ISchemaStorage storage = storageProviderManager.createStorage(providerName, ISchemaStorage.class);
        storage.initialize();

        // load default schemas
        try {
            Resource[] resources = new PathMatchingResourcePatternResolver()
                .getResources("classpath:/schema/*.json");
            for (Resource resource : resources) {
                storage.putIfNotExist(resource.getFilename().replace(".json", ""), StringUtils.from(resource.getInputStream()));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return storage;
    }

    @Bean
    @ConditionalOnProperty(value = "bithon.storage.meta.enabled", havingValue = "true")
    public IMetaStorage metaStorage(MetaStorageConfig storageConfig,
                                    StorageProviderManager storageProviderManager) throws IOException {
        String providerName = StringUtils.isEmpty(storageConfig.getProvider()) ? storageConfig.getType() : storageConfig.getProvider();
        InvalidConfigurationException.throwIf(!StringUtils.hasText(providerName),
                                              "[%s] is not properly configured to enable the meta storage.",
                                              storageConfig.getClass(),
                                              "provider");

        IMetaStorage storage = new CacheableMetadataStorage(storageProviderManager.createStorage(providerName, IMetaStorage.class));
        storage.initialize();
        return storage;
    }

    @Bean
    @ConditionalOnBean(ISchemaStorage.class)
    SchemaManager schemaManager(ISchemaStorage schemaStorage) {
        return new SchemaManager(schemaStorage);
    }

    @Bean
    @ConditionalOnProperty(value = "bithon.storage.tracing.enabled", havingValue = "true")
    public ITraceStorage traceStorage(TraceStorageConfig storageConfig,
                                      StorageProviderManager storageProviderManager,
                                      SchemaManager schemaManager) throws IOException {
        String providerName = StringUtils.isEmpty(storageConfig.getProvider()) ? storageConfig.getType() : storageConfig.getProvider();
        InvalidConfigurationException.throwIf(!StringUtils.hasText(providerName),
                                              "[%s] is not properly configured to enable the Tracing module.",
                                              storageConfig.getClass(),
                                              "provider");

        ITraceStorage storage = storageProviderManager.createStorage(providerName, ITraceStorage.class);
        storage.initialize();

        schemaManager.addSchema(TraceTableSchema.createSummaryTableSchema(storage), false);
        schemaManager.addSchema(TraceTableSchema.createIndexTableSchema(storage, storageConfig.getIndexes()), false);
        return storage;
    }

    @Bean
    @ConditionalOnProperty(value = "bithon.storage.event.enabled", havingValue = "true")
    public IEventStorage eventStorage(EventStorageConfig storageConfig,
                                      StorageProviderManager storageProviderManager,
                                      SchemaManager schemaManager) throws IOException {
        String providerName = StringUtils.isEmpty(storageConfig.getProvider()) ? storageConfig.getType() : storageConfig.getProvider();
        InvalidConfigurationException.throwIf(!StringUtils.hasText(providerName),
                                              "[%s] is not properly configured to enable the Event module.",
                                              storageConfig.getClass(),
                                              "provider");

        IEventStorage storage = storageProviderManager.createStorage(providerName, IEventStorage.class);
        storage.initialize();

        schemaManager.addSchema(EventTableSchema.createEventTableSchema(storage), false);
        return storage;
    }

    @Bean
    @ConditionalOnProperty(value = "bithon.storage.setting.enabled", havingValue = "true")
    public ISettingStorage settingStorage(SettingStorageConfig storageConfig,
                                          StorageProviderManager storageProviderManager) throws IOException {
        String providerName = StringUtils.isEmpty(storageConfig.getProvider()) ? storageConfig.getType() : storageConfig.getProvider();
        InvalidConfigurationException.throwIf(!StringUtils.hasText(providerName),
                                              "[%s] is not properly configured to enable the Setting module.",
                                              storageConfig.getClass(),
                                              "provider");

        ISettingStorage storage = storageProviderManager.createStorage(providerName, ISettingStorage.class);
        storage.initialize();
        return storage;
    }
}
