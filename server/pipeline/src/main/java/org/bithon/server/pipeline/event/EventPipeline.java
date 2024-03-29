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

package org.bithon.server.pipeline.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.bithon.server.pipeline.common.pipeline.AbstractPipeline;
import org.bithon.server.pipeline.event.exporter.IEventExporter;
import org.bithon.server.pipeline.event.metrics.MetricOverEventInputSource;
import org.bithon.server.pipeline.event.receiver.IEventReceiver;
import org.bithon.server.pipeline.metrics.input.MetricInputSourceManager;
import org.bithon.server.storage.event.EventMessage;
import org.slf4j.Logger;

import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 12/4/22 11:38 AM
 */
@Slf4j
public class EventPipeline extends AbstractPipeline<IEventReceiver, IEventExporter> {

    private final MetricInputSourceManager metricInputSourceManager;

    public EventPipeline(EventPipelineConfig pipelineConfig,
                         MetricInputSourceManager metricInputSourceManager,
                         ObjectMapper objectMapper) {
        super(IEventReceiver.class, IEventExporter.class, pipelineConfig, objectMapper);
        this.metricInputSourceManager = metricInputSourceManager;
    }

    public EventPipelineConfig getPipelineConfig() {
        return (EventPipelineConfig) pipelineConfig;
    }

    @Override
    protected void registerProcessor() {
        // Load schemas and register processor for each schema
        this.metricInputSourceManager.start(MetricOverEventInputSource.class);

        IEventProcessor processor = new IEventProcessor() {
            @Override
            public void process(String messageType, List<EventMessage> messages) {
                IEventExporter[] exporterList = exporters.toArray(new IEventExporter[0]);
                for (IEventExporter exporter : exporterList) {
                    try {
                        exporter.process(messageType, messages);
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }
                }
            }

            @Override
            public void close() {
            }
        };

        for (IEventReceiver receiver : this.receivers) {
            receiver.registerProcessor(processor);
        }
    }

    @Override
    protected Logger getLogger() {
        return log;
    }
}
