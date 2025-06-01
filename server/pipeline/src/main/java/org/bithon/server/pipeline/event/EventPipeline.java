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
import org.bithon.server.pipeline.common.input.IInputSourceManager;
import org.bithon.server.pipeline.common.pipeline.AbstractPipeline;
import org.bithon.server.pipeline.event.exporter.IEventExporter;
import org.bithon.server.pipeline.event.exporter.MetricOverEventExporter;
import org.bithon.server.pipeline.event.input.EventAsInputSource;
import org.bithon.server.pipeline.event.receiver.IEventReceiver;
import org.bithon.server.storage.event.EventMessage;
import org.slf4j.Logger;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 12/4/22 11:38 AM
 */
@Slf4j
public class EventPipeline extends AbstractPipeline<IEventReceiver, IEventExporter> {

    private final IInputSourceManager inputSourceManager;
    private final Set<String> metricEvents = new ConcurrentSkipListSet<>();

    public EventPipeline(EventPipelineConfig pipelineConfig,
                         IInputSourceManager inputSourceManager,
                         ObjectMapper objectMapper) {
        super(IEventReceiver.class, IEventExporter.class, pipelineConfig, objectMapper);
        this.inputSourceManager = inputSourceManager;
    }

    public EventPipelineConfig getPipelineConfig() {
        return (EventPipelineConfig) pipelineConfig;
    }

    @Override
    protected void registerProcessor() {
        // Load schemas and register processor for each schema
        this.inputSourceManager.start(EventAsInputSource.class);

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

    /**
     * Previously, some metrics are reported via the event channel.
     * Since these metrics will be exported to a dedicated storage,
     * there's no need to store these events in the default event store.
     *
     * In the future, we can change the reporting channel so that we don't need to keep the implementation here.
     */
    @Override
    public IEventExporter link(IEventExporter exporter) {
        if (exporter instanceof MetricOverEventExporter) {
            String eventType = ((MetricOverEventExporter) exporter).getEventType();
            this.metricEvents.add(eventType);

            return super.link(exporter);
        } else {
            IEventExporter filteredExporter = new IEventExporter() {
                @Override
                public void process(String messageType, List<EventMessage> messages) {
                    if (!metricEvents.isEmpty()) {
                        messages = messages.stream()
                                           .filter((msg) -> !metricEvents.contains(msg.getType()))
                                           .collect(Collectors.toList());
                    }
                    exporter.process(messageType, messages);
                }

                @Override
                public void close() throws Exception {
                    exporter.close();
                }

                @Override
                public void start() {
                    exporter.start();
                }

                @Override
                public void stop() {
                    exporter.stop();
                }

                @Override
                public String toString() {
                    return exporter.toString();
                }
            };
            return super.link(filteredExporter);
        }
    }

    @Override
    public IEventExporter unlink(IEventExporter exporter) {
        if (exporter instanceof MetricOverEventExporter) {
            String eventType = ((MetricOverEventExporter) exporter).getEventType();
            this.metricEvents.remove(eventType);
        }

        return super.unlink(exporter);
    }

    @Override
    protected Logger getLogger() {
        return log;
    }
}
