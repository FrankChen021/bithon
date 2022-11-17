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
import org.bithon.server.sink.metrics.MetricInputSource;
import org.bithon.server.sink.metrics.topo.TopoTransformers;
import org.bithon.server.sink.metrics.transformer.ConnectionStringTransformer;
import org.bithon.server.sink.metrics.transformer.ExtractHost;
import org.bithon.server.sink.metrics.transformer.ExtractPath;
import org.bithon.server.sink.metrics.transformer.UriNormalizationTransformer;
import org.bithon.server.sink.tracing.TraceConfig;
import org.bithon.server.sink.tracing.TraceDataSourceSchemaInitializer;
import org.bithon.server.sink.tracing.metrics.MetricOverSpanInputSource;
import org.bithon.server.storage.datasource.DataSourceSchemaManager;
import org.bithon.server.storage.event.IEventStorage;
import org.bithon.server.storage.meta.IMetaStorage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author frank.chen021@outlook.com
 * @date 3/4/22 11:37 AM
 */

@Configuration(proxyBeanMethods = false)
public class SinkAutoConfiguration {

    /**
     * ConditionalOnBean(DataSourceSchemaManager.class) seems does not work.
     * Use bean initialization mechanism to initialize the related data source schema
     */
    @Bean
    TraceDataSourceSchemaInitializer traceDataSourceSchemaInitializer(TraceConfig traceConfig) {
        return new TraceDataSourceSchemaInitializer(traceConfig);
    }

    @Bean
    TopoTransformers topoTransformers(IMetaStorage metaStorage) {
        return new TopoTransformers(metaStorage);
    }

    @Bean
    EventMessageHandlers eventMessageHandlers(IEventStorage handlers) {
        return new EventMessageHandlers(handlers);
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
                context.registerSubtypes(MetricOverSpanInputSource.class,
                                         MetricInputSource.class,
                                         EventInputSource.class,

                                         // transformers
                                         UriNormalizationTransformer.class,
                                         ExtractHost.class,
                                         ConnectionStringTransformer.class,
                                         ExtractPath.class
                );
            }
        };
    }

    @Bean
    InputSourceManager inputSourceManager(DataSourceSchemaManager dataSourceSchemaManager, ObjectMapper objectMapper) {
        return new InputSourceManager(dataSourceSchemaManager, objectMapper);
    }

    @Bean
    @ConditionalOnProperty(value = "collector-kafka.enabled", havingValue = "true", matchIfMissing = false)
    KafkaConsumerManager kafkaConsumerManager() {
        return new KafkaConsumerManager();
    }
}
