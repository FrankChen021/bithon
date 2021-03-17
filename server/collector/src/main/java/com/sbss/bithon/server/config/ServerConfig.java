package com.sbss.bithon.server.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sbss.bithon.server.collector.sink.IMessageSink;
import com.sbss.bithon.server.collector.sink.local.LocalEventSink;
import com.sbss.bithon.server.collector.sink.local.LocalMetricSink;
import com.sbss.bithon.server.collector.sink.local.LocalTraceSink;
import com.sbss.bithon.server.events.storage.IEventStorage;
import com.sbss.bithon.server.events.storage.jdbc.EventJdbcStorage;
import com.sbss.bithon.server.meta.storage.CachableMetadataStorage;
import com.sbss.bithon.server.meta.storage.IMetaStorage;
import com.sbss.bithon.server.meta.storage.jdbc.MetadataJdbcStorage;
import com.sbss.bithon.server.metric.handler.ExceptionMetricMessageHandler;
import com.sbss.bithon.server.metric.handler.HttpClientMetricMessageHandler;
import com.sbss.bithon.server.metric.handler.JdbcPoolMetricMessageHandler;
import com.sbss.bithon.server.metric.handler.JvmGcMetricMessageHandler;
import com.sbss.bithon.server.metric.handler.JvmMetricMessageHandler;
import com.sbss.bithon.server.metric.handler.RedisMetricMessageHandler;
import com.sbss.bithon.server.metric.handler.ThreadPoolMetricMessageHandler;
import com.sbss.bithon.server.metric.handler.WebRequestMetricMessageHandler;
import com.sbss.bithon.server.metric.handler.WebServerMetricMessageHandler;
import com.sbss.bithon.server.metric.storage.IMetricStorage;
import com.sbss.bithon.server.metric.storage.jdbc.MetricJdbcStorage;
import com.sbss.bithon.server.tracing.handler.TraceMessageHandler;
import com.sbss.bithon.server.tracing.storage.ITraceStorage;
import com.sbss.bithon.server.tracing.storage.jdbc.TraceJdbcStorage;
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
