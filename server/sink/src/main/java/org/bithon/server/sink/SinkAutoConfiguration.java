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

package org.bithon.server.sink;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bithon.server.kafka.KafkaConsumerManager;
import org.bithon.server.sink.common.input.InputSourceManager;
import org.bithon.server.sink.event.EventInputSource;
import org.bithon.server.sink.event.EventMessageHandlers;
import org.bithon.server.sink.event.EventSinkConfig;
import org.bithon.server.sink.metrics.MetricInputSource;
import org.bithon.server.sink.metrics.transformer.ConnectionStringTransformer;
import org.bithon.server.sink.metrics.transformer.ExtractHost;
import org.bithon.server.sink.metrics.transformer.ExtractPath;
import org.bithon.server.sink.metrics.transformer.UriNormalizationTransformer;
import org.bithon.server.sink.tracing.LocalTraceSink;
import org.bithon.server.sink.tracing.TraceSinkConfig;
import org.bithon.server.sink.tracing.metrics.MetricOverSpanInputSource;
import org.bithon.server.sink.tracing.transform.sanitization.UrlSanitizer;
import org.bithon.server.sink.tracing.transform.TraceSpanTransformer;
import org.bithon.server.storage.StorageModuleAutoConfiguration;
import org.bithon.server.storage.datasource.DataSourceSchemaManager;
import org.bithon.server.storage.event.IEventStorage;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

/**
 * @author frank.chen021@outlook.com
 * @date 3/4/22 11:37 AM
 */
@Configuration(proxyBeanMethods = false)
@AutoConfigureAfter(StorageModuleAutoConfiguration.class)
@Conditional(SinkModuleEnabler.class)
public class SinkAutoConfiguration {

    @Bean
    EventMessageHandlers eventMessageHandlers(IEventStorage eventStorage, EventSinkConfig eventSinkConfig) {
        return new EventMessageHandlers(eventStorage, eventSinkConfig);
    }

    @Bean
    public Module sinkModule() {
        return new Module() {
            @Override
            public String getModuleName() {
                return "sink";
            }

            @Override
            public Version version() {
                return Version.unknownVersion();
            }

            @Override
            public void setupModule(SetupContext context) {
                context.registerSubtypes(UrlSanitizer.class,

                                         MetricOverSpanInputSource.class,
                                         MetricInputSource.class,
                                         EventInputSource.class,

                                         // metric transformers
                                         UriNormalizationTransformer.class,
                                         ExtractHost.class,
                                         ConnectionStringTransformer.class,
                                         ExtractPath.class,

                                         // tracing transformers
                                         TraceSpanTransformer.class
                                        );
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(value = "bithon.sinks.tracing.enabled", havingValue = "true", matchIfMissing = true)
    public LocalTraceSink localTraceSink(TraceSinkConfig traceConfig, ObjectMapper om, ApplicationContext applicationContext) {
        return new LocalTraceSink(traceConfig, om, applicationContext);
    }

    /**
     * Input source manager is responsible for hooking the processors on metrics and trace handlers.
     * So all its dependencies like {@link LocalTraceSink} should be prepared.
     * <p>
     * If the sink is kafka, {@link LocalTraceSink} is initialized above
     * If the sink is local, it's initialized in brpc autoconfiguration.
     */
    @Bean
    InputSourceManager inputSourceManager(DataSourceSchemaManager dataSourceSchemaManager, ObjectMapper objectMapper) {
        return new InputSourceManager(dataSourceSchemaManager, objectMapper);
    }

    /**
     * Initialize the consumers at last
     */
    @Bean
    @ConditionalOnProperty(value = "collector-kafka.enabled", havingValue = "true", matchIfMissing = false)
    KafkaConsumerManager kafkaConsumerManager() {
        return new KafkaConsumerManager();
    }
}
