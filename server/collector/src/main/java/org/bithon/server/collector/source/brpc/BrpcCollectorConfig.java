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

package org.bithon.server.collector.source.brpc;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.bithon.server.collector.sink.SinkConfig;
import org.bithon.server.event.sink.IEventMessageSink;
import org.bithon.server.metric.sink.IMessageSink;
import org.bithon.server.metric.sink.IMetricMessageSink;
import org.bithon.server.metric.sink.LocalSchemaMetricSink;
import org.bithon.server.metric.sink.SchemaMetricMessage;
import org.bithon.server.tracing.sink.ITraceMessageSink;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/23 11:45 下午
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "collector-brpc")
@ConditionalOnProperty(value = "collector-brpc.enabled", havingValue = "true", matchIfMissing = false)
public class BrpcCollectorConfig {
    private Map<String, Integer> port;
    private SinkConfig sink;

    @Bean("schemaMetricSink")
    public IMessageSink<SchemaMetricMessage> metricSink(BrpcCollectorConfig config,
                                                        ApplicationContext applicationContext) {
        if ("local".equals(config.getSink().getType())) {
            return new LocalSchemaMetricSink(applicationContext);
        } else {
            // TODO
            return null;
        }
    }

    @Bean
    public IMetricMessageSink metricSink(BrpcCollectorConfig config,
                                         ObjectMapper om) throws IOException {
        return SinkConfig.createSink(config.getSink(), om, IMetricMessageSink.class);
    }

    @Bean
    public IEventMessageSink eventSink(BrpcCollectorConfig config,
                                       ObjectMapper om) throws IOException {
        return SinkConfig.createSink(config.getSink(), om, IEventMessageSink.class);
    }

    @Bean
    public ITraceMessageSink traceSink(BrpcCollectorConfig config,
                                       ObjectMapper om) throws IOException {
        return SinkConfig.createSink(config.getSink(), om, ITraceMessageSink.class);
    }
}
