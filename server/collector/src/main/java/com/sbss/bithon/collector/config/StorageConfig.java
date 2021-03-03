package com.sbss.bithon.collector.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sbss.bithon.collector.datasource.storage.IMetricStorage;
import com.sbss.bithon.collector.datasource.storage.jdbc.MetricsJdbcStorage;
import com.sbss.bithon.collector.events.storage.IEventStorage;
import com.sbss.bithon.collector.events.storage.jdbc.EventJdbcStorage;
import com.sbss.bithon.collector.meta.IMetaStorage;
import com.sbss.bithon.collector.meta.jdbc.MetadataJdbcStorage;
import com.sbss.bithon.collector.tracing.storage.ITraceStorage;
import com.sbss.bithon.collector.tracing.storage.jdbc.TraceJdbcStorage;
import org.jooq.DSLContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/30 8:34 下午
 */
@Configuration
public class StorageConfig {
    @Bean
    public IMetricStorage createStorage(DSLContext dslContext) {
        return new MetricsJdbcStorage(dslContext);
    }

    @Bean
    public IMetaStorage metaStorage(DSLContext dslContext) {
        return new MetadataJdbcStorage(dslContext);
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
