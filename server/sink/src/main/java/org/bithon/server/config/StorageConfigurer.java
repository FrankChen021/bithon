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

package org.bithon.server.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.event.storage.EventStorageConfig;
import org.bithon.server.event.storage.IEventStorage;
import org.bithon.server.meta.storage.CachableMetadataStorage;
import org.bithon.server.meta.storage.IMetaStorage;
import org.bithon.server.metric.storage.IMetricStorage;
import org.bithon.server.metric.storage.MetricStorageConfig;
import org.bithon.server.setting.storage.ISettingStorage;
import org.bithon.server.setting.storage.SettingStorageConfig;
import org.bithon.server.tracing.storage.ITraceStorage;
import org.bithon.server.tracing.storage.TraceStorageConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.Locale;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/30 8:34 下午
 */
@Configuration
public class StorageConfigurer {

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
}
