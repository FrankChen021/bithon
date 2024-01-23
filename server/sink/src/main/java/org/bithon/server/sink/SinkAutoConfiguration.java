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
import org.bithon.server.sink.common.input.InputSourceManager;
import org.bithon.server.sink.event.EventPipeline;
import org.bithon.server.sink.event.EventPipelineConfig;
import org.bithon.server.sink.event.metrics.EventInputSource;
import org.bithon.server.sink.metrics.MetricInputSource;
import org.bithon.server.sink.metrics.MetricMessagePipeline;
import org.bithon.server.sink.metrics.MetricPipelineConfig;
import org.bithon.server.sink.metrics.transform.ConnectionStringTransformer;
import org.bithon.server.sink.metrics.transform.ExtractHost;
import org.bithon.server.sink.metrics.transform.ExtractPath;
import org.bithon.server.sink.metrics.transform.UriNormalizationTransformer;
import org.bithon.server.sink.tracing.TracePipeline;
import org.bithon.server.sink.tracing.TracePipelineConfig;
import org.bithon.server.sink.tracing.metrics.MetricOverSpanInputSource;
import org.bithon.server.sink.tracing.transform.TraceSpanTransformer;
import org.bithon.server.sink.tracing.transform.sanitization.UrlSanitizer;
import org.bithon.server.storage.StorageModuleAutoConfiguration;
import org.bithon.server.storage.datasource.DataSourceSchemaManager;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author frank.chen021@outlook.com
 * @date 3/4/22 11:37 AM
 */
@Configuration(proxyBeanMethods = false)
@AutoConfigureAfter(StorageModuleAutoConfiguration.class)
public class SinkAutoConfiguration {

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
    public EventPipeline eventPipeline(EventPipelineConfig pipelineConfig,
                                       ObjectMapper om) {
        return new EventPipeline(pipelineConfig, om);
    }

    @Bean
    public MetricMessagePipeline metricPipeline(MetricPipelineConfig pipelineConfig,
                                                ObjectMapper om,
                                                ApplicationContext applicationContext) {
        return new MetricMessagePipeline(pipelineConfig, om, applicationContext);
    }

    /**
     * Always create the bean because it might be used in other modules
     */
    @Bean
    public TracePipeline tracePipeline(TracePipelineConfig pipelineConfig,
                                       ObjectMapper om,
                                       ApplicationContext applicationContext) {
        return new TracePipeline(pipelineConfig, om, applicationContext);
    }

    /**
     * Input source manager is responsible for hooking the processors on metrics and trace handlers.
     * So all its dependencies like {@link TracePipeline} should be prepared.
     * <p>
     * If the sink is kafka, {@link TracePipeline} is initialized above
     * If the sink is local, it's initialized in brpc autoconfiguration.
     */
    @Bean
    InputSourceManager inputSourceManager(DataSourceSchemaManager dataSourceSchemaManager, ObjectMapper objectMapper) {
        return new InputSourceManager(dataSourceSchemaManager, objectMapper);
    }
}
