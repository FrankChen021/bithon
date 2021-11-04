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
import org.bithon.server.event.storage.IEventStorage;
import org.bithon.server.meta.storage.CachableMetadataStorage;
import org.bithon.server.meta.storage.IMetaStorage;
import org.bithon.server.metric.storage.IMetricStorage;
import org.bithon.server.setting.storage.ISettingStorage;
import org.bithon.server.tracing.storage.ITraceStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/30 8:34 下午
 */
@Configuration
public class StorageConfigurer {

    @Bean
    public IMetricStorage createMetricStorage(ObjectMapper om, @Value("${bithon.storage.metric}") String metricType) throws IOException {
        String jsonType = String.format("{\"type\":\"%s\"}", metricType);
        IMetricStorage storage = om.readValue(jsonType, IMetricStorage.class);
        storage.initialize();
        return storage;
    }

    @Bean
    public IMetaStorage metaStorage(ObjectMapper om, @Value("${bithon.storage.meta}") String metaType) throws IOException {
        String jsonType = String.format("{\"type\":\"%s\"}", metaType);
        IMetaStorage storage = new CachableMetadataStorage(om.readValue(jsonType, IMetaStorage.class));
        storage.initialize();
        return storage;
    }

    @Bean
    public ITraceStorage traceStorage(ObjectMapper om, @Value("${bithon.storage.tracing}") String tracingType) throws IOException {
        String jsonType = String.format("{\"type\":\"%s\"}", tracingType);
        ITraceStorage storage = om.readValue(jsonType, ITraceStorage.class);
        storage.initialize();
        return storage;
    }

    @Bean
    public IEventStorage eventStorage(ObjectMapper om, @Value("${bithon.storage.event}") String eventType) throws IOException {
        String jsonType = String.format("{\"type\":\"%s\"}", eventType);
        IEventStorage storage = om.readValue(jsonType, IEventStorage.class);
        storage.initialize();
        return storage;
    }

    @Bean
    public ISettingStorage settingStorage(ObjectMapper om, @Value("${bithon.storage.setting}") String type) throws IOException {
        String jsonType = String.format("{\"type\":\"%s\"}", type);
        ISettingStorage storage = om.readValue(jsonType, ISettingStorage.class);
        storage.initialize();
        return storage;
    }
}
