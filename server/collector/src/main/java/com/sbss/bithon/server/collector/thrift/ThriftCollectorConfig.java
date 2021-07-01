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

package com.sbss.bithon.server.collector.thrift;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.sbss.bithon.server.collector.sink.IMessageSink;
import com.sbss.bithon.server.collector.sink.kafka.KafkaEventSink;
import com.sbss.bithon.server.collector.sink.kafka.KafkaMetricSink;
import com.sbss.bithon.server.collector.sink.kafka.KafkaTraceSink;
import com.sbss.bithon.server.collector.sink.local.LocalEventSink;
import com.sbss.bithon.server.collector.sink.local.LocalMetricSink;
import com.sbss.bithon.server.collector.sink.local.LocalTraceSink;
import com.sbss.bithon.server.common.utils.collection.CloseableIterator;
import com.sbss.bithon.server.event.handler.EventsMessageHandler;
import com.sbss.bithon.server.metric.handler.ExceptionMetricMessageHandler;
import com.sbss.bithon.server.metric.handler.GenericMetricMessage;
import com.sbss.bithon.server.metric.handler.HttpClientMetricMessageHandler;
import com.sbss.bithon.server.metric.handler.JdbcPoolMetricMessageHandler;
import com.sbss.bithon.server.metric.handler.JvmGcMetricMessageHandler;
import com.sbss.bithon.server.metric.handler.JvmMetricMessageHandler;
import com.sbss.bithon.server.metric.handler.MongoDbMetricMessageHandler;
import com.sbss.bithon.server.metric.handler.RedisMetricMessageHandler;
import com.sbss.bithon.server.metric.handler.SqlMetricMessageHandler;
import com.sbss.bithon.server.metric.handler.ThreadPoolMetricMessageHandler;
import com.sbss.bithon.server.metric.handler.WebRequestMetricMessageHandler;
import com.sbss.bithon.server.metric.handler.WebServerMetricMessageHandler;
import com.sbss.bithon.server.tracing.handler.TraceMessageHandler;
import lombok.Data;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/23 11:45 下午
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "collector-thrift")
@ConditionalOnProperty(value = "collector-thrift.enabled", havingValue = "true", matchIfMissing = false)
public class ThriftCollectorConfig {
    private Map<String, Integer> port;
    private SinkConfig sink;

    @Data
    static class SinkConfig {
        private String type;
        private Map<String, Object> props;
    }

    @Bean("metricSink")
    public IMessageSink<CloseableIterator<GenericMetricMessage>> metricSink(ThriftCollectorConfig config,
                                                                            ObjectMapper om,
                                                                            JvmMetricMessageHandler jvmMetricMessageHandler,
                                                                            JvmGcMetricMessageHandler jvmGcMetricMessageHandler,
                                                                            WebRequestMetricMessageHandler webRequestMetricMessageHandler,
                                                                            WebServerMetricMessageHandler webServerMetricMessageHandler,
                                                                            ExceptionMetricMessageHandler exceptionMetricMessageHandler,
                                                                            HttpClientMetricMessageHandler httpClientMetricMessageHandler,
                                                                            ThreadPoolMetricMessageHandler threadPoolMetricMessageHandler,
                                                                            JdbcPoolMetricMessageHandler jdbcPoolMetricMessageHandler,
                                                                            RedisMetricMessageHandler redisMetricMessageHandler,
                                                                            SqlMetricMessageHandler sqlMetricMessageHandler,
                                                                            MongoDbMetricMessageHandler mongoDbMetricMessageHandler) {
        if ("local".equals(config.getSink().getType())) {
            return new LocalMetricSink(jvmMetricMessageHandler,
                                       jvmGcMetricMessageHandler,
                                       webRequestMetricMessageHandler,
                                       webServerMetricMessageHandler,
                                       exceptionMetricMessageHandler,
                                       httpClientMetricMessageHandler,
                                       threadPoolMetricMessageHandler,
                                       jdbcPoolMetricMessageHandler,
                                       redisMetricMessageHandler,
                                       sqlMetricMessageHandler,
                                       mongoDbMetricMessageHandler);
        } else {
            return new KafkaMetricSink(new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(config.getSink()
                                                                                                   .getProps(),
                                                                                             new StringSerializer(),
                                                                                             new StringSerializer()),
                                                           ImmutableMap.of("client.id", "metric")),
                                       om);
        }
    }

    @Bean("eventSink")
    public IMessageSink<?> eventSink(ThriftCollectorConfig config,
                                     EventsMessageHandler handler,
                                     ObjectMapper om) {
        if ("local".equals(config.getSink().getType())) {
            return new LocalEventSink(handler);
        } else {
            return new KafkaEventSink(new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(config.getSink().getProps(),
                                                                                            new StringSerializer(),
                                                                                            new StringSerializer()),
                                                          ImmutableMap.of("client.id", "event")),
                                      om);
        }
    }

    @Bean("traceSink")
    public IMessageSink<?> traceSink(ThriftCollectorConfig config,
                                     TraceMessageHandler traceMessageHandler,
                                     ObjectMapper om) {

        if ("local".equals(config.getSink().getType())) {
            return new LocalTraceSink(traceMessageHandler);
        } else {
            return new KafkaTraceSink(new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(config.getSink().getProps(),
                                                                                            new StringSerializer(),
                                                                                            new StringSerializer()),
                                                          ImmutableMap.of("client.id", "trace")),
                                      om);
        }
    }
}
