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

package org.bithon.server.pipeline.metrics;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.bithon.server.pipeline.common.pipeline.AbstractPipeline;
import org.bithon.server.pipeline.metrics.exporter.IMetricExporter;
import org.bithon.server.pipeline.metrics.receiver.IMetricReceiver;
import org.slf4j.Logger;

/**
 * @author frank.chen021@outlook.com
 * @date 3/10/21 14:11
 */
@Slf4j
public class MetricPipeline extends AbstractPipeline<IMetricReceiver, IMetricExporter> {

    public MetricPipeline(MetricPipelineConfig pipelineConfig,
                          ObjectMapper objectMapper) {
        super(IMetricReceiver.class, IMetricExporter.class, pipelineConfig, objectMapper);
    }

    @Override
    protected void registerProcessor() {
        IMetricProcessor processor = new IMetricProcessor() {
            @Override
            public void close() {
            }

            @Override
            public void process(String messageType, SchemaMetricMessage message) {
                IMetricExporter[] exporter = exporters.toArray(new IMetricExporter[0]);
                for (IMetricExporter sink : exporter) {
                    try {
                        sink.process(messageType, message);
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }
                }
            }
        };

        for (IMetricReceiver receiver : this.receivers) {
            receiver.registerProcessor(processor);
        }
    }

    @Override
    protected Logger getLogger() {
        return log;
    }
}
