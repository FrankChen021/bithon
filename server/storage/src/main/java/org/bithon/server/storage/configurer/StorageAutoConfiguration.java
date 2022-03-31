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

package org.bithon.server.storage.configurer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.storage.datasource.DataSourceSchema;
import org.bithon.server.storage.datasource.DataSourceSchemaManager;
import org.bithon.server.storage.datasource.TimestampSpec;
import org.bithon.server.storage.datasource.aggregator.spec.CountMetricSpec;
import org.bithon.server.storage.datasource.aggregator.spec.LongSumMetricSpec;
import org.bithon.server.storage.datasource.dimension.StringDimensionSpec;
import org.bithon.server.storage.event.EventStorageConfig;
import org.bithon.server.storage.event.IEventStorage;
import org.bithon.server.storage.meta.CachableMetadataStorage;
import org.bithon.server.storage.meta.IMetaStorage;
import org.bithon.server.storage.meta.ISchemaStorage;
import org.bithon.server.storage.metrics.IMetricStorage;
import org.bithon.server.storage.metrics.MetricStorageConfig;
import org.bithon.server.storage.setting.ISettingStorage;
import org.bithon.server.storage.setting.SettingStorageConfig;
import org.bithon.server.storage.tracing.ITraceStorage;
import org.bithon.server.storage.tracing.TraceStorageConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/30 8:34 下午
 */
@Configuration
public class StorageAutoConfiguration {

    @Bean
    public IMetricStorage createMetricStorage(ObjectMapper om, MetricStorageConfig storageConfig) throws IOException {
        InvalidConfigurationException.throwIf(!StringUtils.hasText(storageConfig.getType()),
                                              "[%s] can't be blank",
                                              storageConfig.getClass(),
                                              "type");

        String jsonType = String.format(Locale.ENGLISH, "{\"type\":\"%s\"}", storageConfig.getType());
        IMetricStorage storage = om.readValue(jsonType, IMetricStorage.class);
        storage.initialize();
        return storage;
    }

    @Bean
    public ISchemaStorage createSchemaStorage(ObjectMapper om, MetricStorageConfig storageConfig) throws IOException {
        InvalidConfigurationException.throwIf(!StringUtils.hasText(storageConfig.getType()),
                                              "[%s] can't be blank",
                                              storageConfig.getClass(),
                                              "type");

        String jsonType = String.format(Locale.ENGLISH, "{\"type\":\"%s\"}", storageConfig.getType());
        ISchemaStorage storage = om.readValue(jsonType, ISchemaStorage.class);
        storage.initialize();
        return storage;
    }

    @Bean
    public IMetaStorage metaStorage(ObjectMapper om, MetricStorageConfig storageConfig) throws IOException {
        InvalidConfigurationException.throwIf(!StringUtils.hasText(storageConfig.getType()),
                                              "[%s] can't be blank",
                                              storageConfig.getClass(),
                                              "type");

        String jsonType = String.format(Locale.ENGLISH, "{\"type\":\"%s\"}", storageConfig.getType());
        IMetaStorage storage = new CachableMetadataStorage(om.readValue(jsonType, IMetaStorage.class));
        storage.initialize();
        return storage;
    }

    @Bean
    public ITraceStorage traceStorage(ObjectMapper om, TraceStorageConfig storageConfig) throws IOException {
        InvalidConfigurationException.throwIf(!StringUtils.hasText(storageConfig.getType()),
                                              "[%s] can't be blank",
                                              storageConfig.getClass(),
                                              "type");

        String jsonType = String.format(Locale.ENGLISH, "{\"type\":\"%s\"}", storageConfig.getType());
        ITraceStorage storage = om.readValue(jsonType, ITraceStorage.class);
        storage.initialize();
        return storage;
    }

    @Bean
    public IEventStorage eventStorage(ObjectMapper om, EventStorageConfig storageConfig) throws IOException {
        InvalidConfigurationException.throwIf(!StringUtils.hasText(storageConfig.getType()),
                                              "[%s] can't be blank",
                                              storageConfig.getClass(),
                                              "type");

        String jsonType = String.format(Locale.ENGLISH, "{\"type\":\"%s\"}", storageConfig.getType());
        IEventStorage storage = om.readValue(jsonType, IEventStorage.class);
        storage.initialize();
        return storage;
    }

    @Bean
    public ISettingStorage settingStorage(ObjectMapper om, SettingStorageConfig storageConfig) throws IOException {
        InvalidConfigurationException.throwIf(!StringUtils.hasText(storageConfig.getType()),
                                              "[%s] can't be blank",
                                              storageConfig.getClass(),
                                              "type");

        String jsonType = String.format(Locale.ENGLISH, "{\"type\":\"%s\"}", storageConfig.getType());
        ISettingStorage storage = om.readValue(jsonType, ISettingStorage.class);
        storage.initialize();
        return storage;
    }

    private DataSourceSchema createEventTableSchema() {
        return new DataSourceSchema("event",
                                    "event",
                                    new TimestampSpec("timestamp", null, null),
                                    Arrays.asList(new StringDimensionSpec("appName",
                                                                          "appName",
                                                                          "appName",
                                                                          true,
                                                                          null,
                                                                          null,
                                                                          null),
                                                  new StringDimensionSpec("instanceName",
                                                                          "instanceName",
                                                                          "instanceName",
                                                                          false,
                                                                          null,
                                                                          null,
                                                                          null),
                                                  new StringDimensionSpec("type",
                                                                          "type",
                                                                          "type",
                                                                          false,
                                                                          true,
                                                                          null,
                                                                          null)),
                                    Collections.singletonList(CountMetricSpec.INSTANCE));

    }

    private DataSourceSchema createTraceSpanSchema() {
        return new DataSourceSchema("trace_span_summary",
                                    "trace_span_summary",
                                    new TimestampSpec("timestamp", null, null),
                                    Arrays.asList(new StringDimensionSpec("appName",
                                                                          "appName",
                                                                          "appName",
                                                                          true,
                                                                          null,
                                                                          null,
                                                                          null),
                                                  new StringDimensionSpec("instanceName",
                                                                          "instanceName",
                                                                          "instanceName",
                                                                          false,
                                                                          null,
                                                                          null,
                                                                          null),
                                                  new StringDimensionSpec("status",
                                                                          "status",
                                                                          "status",
                                                                          false,
                                                                          true,
                                                                          null,
                                                                          null),
                                                  new StringDimensionSpec("normalizedUrl",
                                                                          "url",
                                                                          "url",
                                                                          false,
                                                                          true,
                                                                          128,
                                                                          null)),
                                    Arrays.asList(CountMetricSpec.INSTANCE,
                                                  new LongSumMetricSpec("costTimeMs",
                                                                        "costTimeMs",
                                                                        "us",
                                                                        true)
                                    ));
    }

    @Bean
    DataSourceSchemaManager schemaManager(ISchemaStorage schemaStorage, ObjectMapper objectMapper) {
        final DataSourceSchema eventTableSchema = createEventTableSchema();
        final DataSourceSchema traceTableSchema = createTraceSpanSchema();

        DataSourceSchemaManager schemaManager = new DataSourceSchemaManager(schemaStorage, objectMapper);

        schemaManager.addDataSourceSchema(eventTableSchema);
        schemaManager.addDataSourceSchema(traceTableSchema);

        schemaManager.addListener(new DataSourceSchemaManager.IDataSourceSchemaListener() {
            @Override
            public void onRmv(DataSourceSchema dataSourceSchema) {
            }

            @Override
            public void onAdd(DataSourceSchema dataSourceSchema) {
            }

            @Override
            public void onRefreshed() {
                schemaManager.addDataSourceSchema(eventTableSchema);
                schemaManager.addDataSourceSchema(traceTableSchema);
            }
        });
        return schemaManager;
    }
}
