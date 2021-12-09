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

package org.bithon.server.event.handler;

import lombok.extern.slf4j.Slf4j;
import org.bithon.server.common.handler.AbstractThreadPoolMessageHandler;
import org.bithon.server.common.utils.collection.CloseableIterator;
import org.bithon.server.event.storage.IEventStorage;
import org.bithon.server.event.storage.IEventWriter;
import org.bithon.server.metric.DataSourceSchema;
import org.bithon.server.metric.DataSourceSchemaManager;
import org.bithon.server.metric.input.InputRow;
import org.bithon.server.metric.storage.IMetricStorage;
import org.bithon.server.metric.storage.IMetricWriter;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/14 4:01 下午
 */
@Slf4j
@Component
public class EventsMessageHandler extends AbstractThreadPoolMessageHandler<CloseableIterator<EventMessage>> {

    final IEventWriter eventWriter;
    final IMetricWriter exceptionMetricWriter;

    public EventsMessageHandler(IEventStorage eventStorage,
                                IMetricStorage metricStorage,
                                DataSourceSchemaManager schemaManager) throws IOException {
        super("event", 1, 5, Duration.ofMinutes(3), 1024);
        this.eventWriter = eventStorage.createWriter();

        DataSourceSchema schema = schemaManager.getDataSourceSchema("exception-metrics");
        schema.setEnforceDuplicationCheck(false);
        this.exceptionMetricWriter = metricStorage.createMetricWriter(schema);
    }

    @Override
    protected void onMessage(CloseableIterator<EventMessage> iterator) throws IOException {
        List<EventMessage> messages = new ArrayList<>();
        List<InputRow> metrics = new ArrayList<>();
        while (iterator.hasNext()) {
            EventMessage message = iterator.next();
            if ("exception".equals(message.getType())) {
                // generate a metric
                InputRow row = new InputRow(new HashMap<>(message.getArgs()));
                row.updateColumn("appName", message.getAppName());
                row.updateColumn("instanceName", message.getInstanceName());
                row.updateColumn("timestamp", message.getTimestamp());
                row.updateColumn("exceptionCount", 1);
                metrics.add(row);
            }
            messages.add(message);
        }
        if (!metrics.isEmpty()) {
            exceptionMetricWriter.write(metrics);
        }
        eventWriter.write(messages);
    }

    @Override
    public String getType() {
        return "event";
    }
}
