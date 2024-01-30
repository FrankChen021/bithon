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

package org.bithon.server.pipeline.event.metrics;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.bithon.server.pipeline.event.exporter.IEventExporter;
import org.bithon.server.pipeline.metrics.MetricPipelineConfig;
import org.bithon.server.pipeline.metrics.exporter.MetricMessageHandler;
import org.bithon.server.storage.datasource.SchemaManager;
import org.bithon.server.storage.datasource.input.IInputRow;
import org.bithon.server.storage.datasource.input.InputRow;
import org.bithon.server.storage.event.EventMessage;
import org.bithon.server.storage.meta.IMetaStorage;
import org.bithon.server.storage.metrics.IMetricStorage;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 2022/8/2 22:30
 */
@Slf4j
public class MetricOverEventHandler implements IEventExporter {

    private final String eventType;
    private final ObjectMapper objectMapper;
    private final MetricMessageHandler metricHandler;

    public MetricOverEventHandler(String eventType,
                                  String dataSourceName,
                                  ObjectMapper objectMapper,
                                  IMetaStorage metaStorage,
                                  IMetricStorage metricStorage,
                                  SchemaManager schemaManager,
                                  MetricPipelineConfig metricPipelineConfig) throws IOException {
        metricHandler = new MetricMessageHandler(dataSourceName,
                                                 metaStorage,
                                                 metricStorage,
                                                 schemaManager,
                                                 null,
                                                 metricPipelineConfig);
        this.eventType = eventType;
        this.objectMapper = objectMapper;
    }

    @Override
    public void process(String messageType, List<EventMessage> messages) {
        List<IInputRow> metricMessages = messages.stream()
                                                 .filter(eventMessage -> this.eventType.equals(eventMessage.getType()))
                                                 .map(eventMessage -> {
                                                     try {
                                                         IInputRow row = new InputRow(objectMapper, objectMapper.readTree(eventMessage.getJsonArgs()));
                                                         row.updateColumn("appName", eventMessage.getAppName());
                                                         row.updateColumn("instanceName", eventMessage.getInstanceName());

                                                         if (row.getCol("timestamp") == null) {
                                                             row.updateColumn("timestamp", eventMessage.getTimestamp());
                                                         }

                                                         row.updateColumn("eventCount", 1);

                                                         return row;
                                                     } catch (JsonProcessingException e) {
                                                         throw new RuntimeException(e);
                                                     }
                                                 })
                                                 .collect(Collectors.toList());

        this.metricHandler.process(metricMessages);
    }

    @Override
    public void close() throws Exception {
        metricHandler.close();
    }

    @Override
    public String toString() {
        return "MetricOverEventHandler[eventType=" + this.eventType + "]";
    }
}
