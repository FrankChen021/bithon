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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.storage.datasource.DataSourceSchema;
import org.bithon.server.storage.datasource.DataSourceSchemaManager;
import org.bithon.server.storage.datasource.TimestampSpec;
import org.bithon.server.storage.datasource.column.IColumn;
import org.bithon.server.storage.datasource.column.StringColumn;
import org.bithon.server.storage.datasource.column.aggregatable.count.AggregateCountColumn;
import org.bithon.server.storage.datasource.column.aggregatable.sum.AggregateLongSumColumn;
import org.bithon.server.storage.event.EventStorageConfig;
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
import org.bithon.server.storage.tracing.index.TagIndexConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/30 8:34 下午
 */
@Configuration
public class StorageAutoConfiguration {

    @Bean
    @ConditionalOnProperty(value = "bithon.storage.metric.enabled", havingValue = "true")
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
    @ConditionalOnProperty(value = "bithon.storage.metric.enabled", havingValue = "true")
    public ISchemaStorage createSchemaStorage(ObjectMapper om, MetricStorageConfig storageConfig) throws IOException {
        InvalidConfigurationException.throwIf(!StringUtils.hasText(storageConfig.getType()),
                                              "[%s] can't be blank",
                                              storageConfig.getClass(),
                                              "type");

        String jsonType = String.format(Locale.ENGLISH, "{\"type\":\"%s\"}", storageConfig.getType());
        ISchemaStorage storage = om.readValue(jsonType, ISchemaStorage.class);
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
    public IMetaStorage metaStorage(ObjectMapper om, MetaStorageConfig storageConfig) throws IOException {
        InvalidConfigurationException.throwIf(!StringUtils.hasText(storageConfig.getType()),
                                              "[%s] can't be blank",
                                              storageConfig.getClass(),
                                              "type");

        String jsonType = String.format(Locale.ENGLISH, "{\"type\":\"%s\"}", storageConfig.getType());
        IMetaStorage storage = new CacheableMetadataStorage(om.readValue(jsonType, IMetaStorage.class));
        storage.initialize();
        return storage;
    }

    @Bean
    @ConditionalOnProperty(value = "bithon.storage.tracing.enabled", havingValue = "true")
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
    @ConditionalOnProperty(value = "bithon.storage.event.enabled", havingValue = "true")
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
    @ConditionalOnProperty(value = "bithon.storage.setting.enabled", havingValue = "true")
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
                                    Arrays.asList(new StringColumn("appName",
                                                                   "appName",
                                                                   "appName",
                                                                   null),
                                                  new StringColumn("instanceName",
                                                                   "instanceName",
                                                                   "instanceName",
                                                                   null),
                                                  new StringColumn("type",
                                                                   "type",
                                                                   "type",
                                                                   true)),
                                    Collections.singletonList(AggregateCountColumn.INSTANCE));

    }

    private DataSourceSchema createTraceSpanSchema() {
        DataSourceSchema dataSourceSchema =
                new DataSourceSchema("trace_span_summary",
                                     "trace_span_summary",
                                     new TimestampSpec("timestamp", null, null),
                                     Arrays.asList(new StringColumn("appName",
                                                                    "appName",
                                                                    "appName",
                                                                    null),
                                                   new StringColumn("instanceName",
                                                                    "instanceName",
                                                                    "instanceName",
                                                                    null),
                                                   new StringColumn("status",
                                                                    "status",
                                                                    "status",
                                                                    true),
                                                   new StringColumn("name", "name", "name", false),
                                                   new StringColumn("normalizedUrl",
                                                                    "url",
                                                                    "url",
                                                                    true),
                                                   new StringColumn("kind", "kind", "kind", false)),
                                     Arrays.asList(AggregateCountColumn.INSTANCE,

                                                   // microsecond
                                                   new AggregateLongSumColumn("costTimeMs",
                                                                              null,
                                                                              "costTimeMs")));
        dataSourceSchema.setVirtual(true);
        return dataSourceSchema;
    }

    private DataSourceSchema createTraceTagIndexSchema(TagIndexConfig tagIndexConfig) {
        List<IColumn> dimensionSpecs = new ArrayList<>();
        if (tagIndexConfig != null) {
            for (Map.Entry<String, Integer> entry : tagIndexConfig.getMap().entrySet()) {
                String tagName = "tags." + entry.getKey();
                Integer indexPos = entry.getValue();
                dimensionSpecs.add(new StringColumn("f" + indexPos,
                                                    // Alias
                                                    tagName,
                                                    // Display
                                                    entry.getKey(),
                                                    null));
            }
        }

        final DataSourceSchema spanTagSchema = new DataSourceSchema("trace_span_tag_index",
                                                                    "trace_span_tag_index",
                                                                    new TimestampSpec("timestamp", null, null),
                                                                    dimensionSpecs,
                                                                    Collections.singletonList(AggregateCountColumn.INSTANCE));
        spanTagSchema.setVirtual(true);
        return spanTagSchema;
    }

    @Bean
    @ConditionalOnBean(ISchemaStorage.class)
    DataSourceSchemaManager schemaManager(ISchemaStorage schemaStorage, TraceStorageConfig traceStorageConfig) {
        DataSourceSchemaManager schemaManager = new DataSourceSchemaManager(schemaStorage);
        schemaManager.addDataSourceSchema(createEventTableSchema());
        schemaManager.addDataSourceSchema(createTraceSpanSchema());
        schemaManager.addDataSourceSchema(createTraceTagIndexSchema(traceStorageConfig.getIndexes()));
        return schemaManager;
    }
}
