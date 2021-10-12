/*
 *    Copyright 2020 bithon.cn
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
import org.bithon.server.event.storage.jdbc.EventJdbcStorage;
import org.bithon.server.meta.storage.CachableMetadataStorage;
import org.bithon.server.meta.storage.IMetaStorage;
import org.bithon.server.meta.storage.jdbc.MetadataJdbcStorage;
import org.bithon.server.metric.storage.IMetricStorage;
import org.bithon.server.metric.storage.jdbc.MetricJdbcStorage;
import org.bithon.server.tracing.storage.ITraceStorage;
import org.bithon.server.tracing.storage.jdbc.TraceJdbcStorage;
import org.jooq.DSLContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * TODO：采用jackson反序列化方式创建，storage的参数放在一起
 *
 * @author frank.chen021@outlook.com
 * @date 2021/1/30 8:34 下午
 */
@Configuration
public class ServerConfig {
    @Bean
    public IMetricStorage createMetricStorage(DSLContext dslContext) {
        return new MetricJdbcStorage(dslContext);
    }

    @Bean
    public IMetaStorage metaStorage(DSLContext dslContext) {
        return new CachableMetadataStorage(new MetadataJdbcStorage(dslContext));
    }

    @Bean
    public ITraceStorage traceStorage(DSLContext dslContext, ObjectMapper objectMapper) {
        return new TraceJdbcStorage(dslContext, objectMapper);
    }

    @Bean
    public IEventStorage eventStorage(DSLContext dslContext, ObjectMapper objectMapper) {
        return new EventJdbcStorage(dslContext, objectMapper);
    }
}
