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

package org.bithon.agent.exporter.brpc;


import org.bithon.agent.observability.exporter.IMessageConverter;
import org.bithon.agent.observability.exporter.IMessageExporter;
import org.bithon.agent.observability.exporter.IMessageExporterFactory;
import org.bithon.agent.observability.exporter.config.ExporterConfig;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/6/27 20:01
 */
public class BrpcExporterFactory implements IMessageExporterFactory {

    static {
        // Make sure the underlying netty use JDK direct memory region so that the memory can be tracked
        System.setProperty("org.bithon.shaded.io.netty.maxDirectMemory", "0");
    }

    @Override
    public IMessageExporter createMetricExporter(ExporterConfig exporterConfig) {
        return new BrpcMetricMessageExporter(exporterConfig);
    }

    @Override
    public IMessageExporter createTracingExporter(ExporterConfig exporterConfig) {
        return new BrpcTraceMessageExporter(exporterConfig);
    }

    @Override
    public IMessageExporter createEventExporter(ExporterConfig exporterConfig) {
        return new BrpcEventMessageExporter(exporterConfig);
    }

    @Override
    public IMessageConverter createMessageConverter() {
        return new BrpcMessageConverter();
    }
}
