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

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.storage.common.provider.StorageProviderManager;
import org.bithon.server.storage.dashboard.DashboardStorageConfig;
import org.bithon.server.storage.dashboard.IDashboardStorage;
import org.bithon.server.storage.datasource.ISchemaStorage;
import org.bithon.server.storage.datasource.SchemaManager;
import org.bithon.server.storage.event.EventStorageConfig;
import org.bithon.server.storage.event.EventTableSchema;
import org.bithon.server.storage.event.IEventStorage;
import org.bithon.server.storage.meta.CacheableMetadataStorage;
import org.bithon.server.storage.meta.IMetaStorage;
import org.bithon.server.storage.meta.MetaStorageConfig;
import org.bithon.server.storage.metrics.IMetricStorage;
import org.bithon.server.storage.metrics.MetricDataSourceSpec;
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
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/30 8:34 下午
 */
@Configuration
public class StorageModuleAutoConfiguration {

    @Bean
    public Module internalDataSourceModule() {
        return new Module() {
            @Override
            public String getModuleName() {
                return "internal-storage-module";
            }

            @Override
            public Version version() {
                return Version.unknownVersion();
            }

            @Override
            public void setupModule(SetupContext context) {
                // For backward compatibility
                context.registerSubtypes(new NamedType(MetricDataSourceSpec.class, "internal"));

                context.registerSubtypes(new NamedType(MetricDataSourceSpec.class, "metric"));
            }
        };
    }

    interface SchemaInitializer {
        void initialize(SchemaManager schemaManager);
    }

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
    @ConditionalOnProperty(value = "bithon.storage.tracing.enabled", havingValue = "true")
    public ITraceStorage traceStorage(TraceStorageConfig storageConfig,
                                      StorageProviderManager storageProviderManager) throws IOException {
        String providerName = StringUtils.isEmpty(storageConfig.getProvider()) ? storageConfig.getType() : storageConfig.getProvider();
        InvalidConfigurationException.throwIf(!StringUtils.hasText(providerName),
                                              "[%s] is not properly configured to enable the Tracing module.",
                                              storageConfig.getClass(),
                                              "provider");

        ITraceStorage storage = storageProviderManager.createStorage(providerName, ITraceStorage.class);
        storage.initialize();
        return storage;
    }

    @Bean
    @ConditionalOnProperty(value = "bithon.storage.tracing.enabled", havingValue = "true")
    SchemaInitializer traceSchemaInitializer(ITraceStorage storage, TraceStorageConfig storageConfig) {
        return new SchemaInitializer() {
            @Override
            public void initialize(SchemaManager schemaManager) {
                schemaManager.addSchema(TraceTableSchema.createSummaryTableSchema(storage), false);
                schemaManager.addSchema(TraceTableSchema.createIndexTableSchema(storage, storageConfig.getIndexes()), false);
            }
        };
    }

    @Bean
    @ConditionalOnProperty(value = "bithon.storage.event.enabled", havingValue = "true")
    SchemaInitializer eventSchemaInitializer(IEventStorage storage) {
        return new SchemaInitializer() {
            @Override
            public void initialize(SchemaManager schemaManager) {
                schemaManager.addSchema(EventTableSchema.createEventTableSchema(storage), false);
            }
        };
    }

    @Bean
    @ConditionalOnProperty(value = "bithon.storage.event.enabled", havingValue = "true")
    public IEventStorage eventStorage(EventStorageConfig storageConfig,
                                      StorageProviderManager storageProviderManager) throws IOException {
        String providerName = StringUtils.isEmpty(storageConfig.getProvider()) ? storageConfig.getType() : storageConfig.getProvider();
        InvalidConfigurationException.throwIf(!StringUtils.hasText(providerName),
                                              "[%s] is not properly configured to enable the Event module.",
                                              storageConfig.getClass(),
                                              "provider");

        IEventStorage storage = storageProviderManager.createStorage(providerName, IEventStorage.class);
        storage.initialize();
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

    @Bean
    @ConditionalOnBean(ISchemaStorage.class)
    SchemaManager schemaManager(ISchemaStorage schemaStorage, List<SchemaInitializer> initializerList) {
        SchemaManager manager = new SchemaManager(schemaStorage);
        for (SchemaInitializer initializer : initializerList) {
            initializer.initialize(manager);
        }
        return manager;
    }

    @Bean
    @ConditionalOnProperty(value = "bithon.storage.dashboard.enabled", havingValue = "true")
    public IDashboardStorage dashboardStorage(ObjectMapper om,
                                              StorageProviderManager storageProviderManager,
                                              DashboardStorageConfig storageConfig) throws IOException {
        String providerName = StringUtils.isEmpty(storageConfig.getProvider()) ? storageConfig.getType() : storageConfig.getProvider();
        InvalidConfigurationException.throwIf(!StringUtils.hasText(providerName),
                                              "[%s] is not properly configured to enable the storage for the dashboard module.",
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
