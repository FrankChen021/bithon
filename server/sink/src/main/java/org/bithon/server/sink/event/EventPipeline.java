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

package org.bithon.server.sink.event;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.utils.CollectionUtils;
import org.bithon.component.commons.utils.Preconditions;
import org.bithon.server.sink.event.exporter.IEventExporter;
import org.bithon.server.sink.event.receiver.IEventReceiver;
import org.bithon.server.storage.event.EventMessage;
import org.springframework.context.SmartLifecycle;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 12/4/22 11:38 AM
 */
@Slf4j
public class EventPipeline implements SmartLifecycle {

    @Getter
    private final boolean isEnabled;

    private final List<IEventReceiver> receivers;
    private final List<IEventExporter> exporters;
    private boolean isRunning = false;

    public EventPipeline(@JacksonInject(useInput = OptBoolean.FALSE) EventPipelineConfig pipelineConfig,
                         @JacksonInject(useInput = OptBoolean.FALSE) ObjectMapper objectMapper) {
        this.isEnabled = pipelineConfig.isEnabled();

        this.receivers = createReceivers(pipelineConfig, objectMapper);
        this.exporters = createExporters(pipelineConfig, objectMapper);
    }

    private <T> T createObject(Class<T> clazz, ObjectMapper objectMapper, Object configuration) throws IOException {
        return objectMapper.readValue(objectMapper.writeValueAsBytes(configuration), clazz);
    }

    private List<IEventReceiver> createReceivers(EventPipelineConfig pipelineConfig,
                                                 ObjectMapper objectMapper) {
        if (!pipelineConfig.isEnabled()) {
            return Collections.emptyList();
        }

        Preconditions.checkIfTrue(!CollectionUtils.isEmpty(pipelineConfig.getReceivers()), "The event pipeline processing is enabled, but no receiver defined.");

        return pipelineConfig.getReceivers()
                             .stream()
                             .map((receiverConfig) -> {
                                 try {
                                     return createObject(IEventReceiver.class, objectMapper, receiverConfig);
                                 } catch (IOException e) {
                                     throw new RuntimeException(e);
                                 }
                             }).collect(Collectors.toList());

    }

    private List<IEventExporter> createExporters(EventPipelineConfig pipelineConfig,
                                                 ObjectMapper objectMapper) {
        if (!pipelineConfig.isEnabled()) {
            // Returns an empty list because other modules can add exporter
            return new ArrayList<>();
        }

        Preconditions.checkIfTrue(!CollectionUtils.isEmpty(pipelineConfig.getExporters()), "The event pipeline processing is enabled, but no exporter defined.");

        // Since the returned export list can be modified,
        // we don't use stream() API because the stream API returns an unmodifiable list
        List<IEventExporter> exporters = new ArrayList<>();
        for (Object exporterConfig : pipelineConfig.getExporters()) {
            try {
                exporters.add(createObject(IEventExporter.class, objectMapper, exporterConfig));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return exporters;
    }


    @Override
    public void start() {
        log.info("Starting the source of event process pipeline...");

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
            receiver.start();
        }
        this.isRunning = true;
    }

    @Override
    public void stop() {
        // Stop the receiver first
        for (IEventReceiver receiver : this.receivers) {
            receiver.stop();
        }

        for (IEventExporter exporter : this.exporters) {
            try {
                exporter.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public boolean isRunning() {
        return isRunning;
    }


    public <T extends IEventExporter> T link(T sink) {
        if (this.isEnabled) {
            synchronized (exporters) {
                exporters.add(sink);
            }
        }
        return sink;
    }

    public <T extends IEventExporter> T unlink(T sink) {
        if (this.isEnabled) {
            synchronized (exporters) {
                exporters.remove(sink);
            }
        }
        return sink;
    }
}
