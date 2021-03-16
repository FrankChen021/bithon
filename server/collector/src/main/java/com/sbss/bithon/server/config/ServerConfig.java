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
import com.sbss.bithon.server.metric.collector.*;
import com.sbss.bithon.server.metric.storage.IMetricStorage;
import com.sbss.bithon.server.metric.storage.jdbc.MetricJdbcStorage;
import com.sbss.bithon.server.tracing.collector.TraceMessageHandler;
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

    @Bean("metricSink")
    public IMessageSink metricSink(JvmMetricMessageHandler jvmMetricMessageHandler,
                                      JvmGcMetricMessageHandler jvmGcMetricMessageHandler,
                                      WebRequestMetricMessageHandler webRequestMetricMessageHandler,
                                      WebServerMetricMessageHandler webServerMetricMessageHandler,
                                      ExceptionMetricMessageHandler exceptionMetricMessageHandler,
                                      HttpClientMetricMessageHandler httpClientMetricMessageHandler,
                                      ThreadPoolMetricMessageHandler threadPoolMetricMessageHandler,
                                      JdbcPoolMetricMessageHandler jdbcPoolMetricMessageHandler,
                                      RedisMetricMessageHandler redisMetricMessageHandler) {
        return new LocalMetricSink(jvmMetricMessageHandler,
                jvmGcMetricMessageHandler,
                webRequestMetricMessageHandler,
                webServerMetricMessageHandler,
                exceptionMetricMessageHandler,
                httpClientMetricMessageHandler,
                threadPoolMetricMessageHandler,
                jdbcPoolMetricMessageHandler,
                redisMetricMessageHandler);
    }

    @Bean("eventSink")
    public IMessageSink eventSink() {
        return new LocalEventSink();
    }

    @Bean("traceSink")
    public IMessageSink traceSink(TraceMessageHandler traceMessageHandler) {
        return new LocalTraceSink(traceMessageHandler);
    }
}
