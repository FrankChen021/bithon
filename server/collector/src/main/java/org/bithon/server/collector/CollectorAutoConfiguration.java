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

package org.bithon.server.collector;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bithon.server.collector.sink.kafka.KafkaEventSink;
import org.bithon.server.collector.sink.kafka.KafkaMetricSink;
import org.bithon.server.collector.sink.kafka.KafkaTraceSink;
import org.bithon.server.collector.source.brpc.BrpcCollectorConfig;
import org.bithon.server.sink.event.IEventMessageSink;
import org.bithon.server.sink.metrics.IMetricMessageSink;
import org.bithon.server.sink.tracing.ITraceMessageSink;
import org.bithon.server.sink.tracing.TraceConfig;
import org.bithon.server.sink.tracing.TraceMessageProcessChain;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import java.io.IOException;

/**
 * @author frank.chen021@outlook.com
 * @date 9/12/21 5:23 PM
 */
@Configuration
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
public class CollectorAutoConfiguration {

    @Bean
    public Module collectorModule() {
        return new Module() {
            @Override
            public String getModuleName() {
                return "sink-kafka";
            }

            @Override
            public Version version() {
                return Version.unknownVersion();
            }

            @Override
            public void setupModule(SetupContext context) {
                context.registerSubtypes(KafkaEventSink.class,
                                         KafkaMetricSink.class,
                                         KafkaTraceSink.class);
            }
        };
    }

    @Bean
    @ConditionalOnProperty(value = "collector-brpc.enabled", havingValue = "true")
    public IMetricMessageSink metricSink(BrpcCollectorConfig config,
                                         ObjectMapper om) throws IOException {
        return config.getSinks().getMetrics().createSink(om, IMetricMessageSink.class);
    }

    @Bean
    @ConditionalOnProperty(value = "collector-brpc.enabled", havingValue = "true")
    public IEventMessageSink eventSink(BrpcCollectorConfig config,
                                       ObjectMapper om) throws IOException {
        return config.getSinks().getEvent().createSink(om, IEventMessageSink.class);
    }

    @Bean
    @ConditionalOnExpression(value = "${collector-brpc.enabled: false} or ${collector-http.enabled: false}")
    public TraceMessageProcessChain traceSink(BrpcCollectorConfig config,
                                              TraceConfig traceConfig,
                                              ObjectMapper om) throws IOException {
        return new TraceMessageProcessChain(traceConfig.createFilter(om),
                                            config.getSinks().getTracing().createSink(om, ITraceMessageSink.class));
    }
}
