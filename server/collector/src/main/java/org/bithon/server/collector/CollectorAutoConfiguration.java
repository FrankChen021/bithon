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
import org.bithon.server.collector.brpc.BrpcEventCollector;
import org.bithon.server.collector.brpc.BrpcMetricCollector;
import org.bithon.server.collector.brpc.BrpcTraceCollector;
import org.bithon.server.collector.http.BithonHttpTraceEnabler;
import org.bithon.server.collector.otlp.grpc.OtlpGrpcTraceReceiver;
import org.bithon.server.collector.otlp.http.OtlpHttpTraceReceiverEnabler;
import org.bithon.server.collector.zipkin.ZipkinHttpTraceReceiverEnabler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author frank.chen021@outlook.com
 * @date 9/12/21 5:23 PM
 */
@Configuration
public class CollectorAutoConfiguration {

    @Bean
    public Module collectorModule() {
        return new Module() {
            @Override
            public String getModuleName() {
                return "bithon-collector";
            }

            @Override
            public Version version() {
                return Version.unknownVersion();
            }

            @Override
            public void setupModule(SetupContext context) {
                context.registerSubtypes(BrpcTraceCollector.class,
                                         BrpcMetricCollector.class,
                                         BrpcEventCollector.class,

                                         BithonHttpTraceEnabler.class,

                                         OtlpHttpTraceReceiverEnabler.class,
                                         OtlpGrpcTraceReceiver.class,

                                         ZipkinHttpTraceReceiverEnabler.class);
            }
        };
    }
}
