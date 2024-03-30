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

package org.bithon.server.pipeline.metrics.exporter;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.OptBoolean;
import lombok.extern.slf4j.Slf4j;
import org.bithon.server.pipeline.metrics.MetricPipelineConfig;
import org.bithon.server.pipeline.metrics.SchemaMetricMessage;
import org.bithon.server.storage.datasource.SchemaManager;
import org.bithon.server.storage.meta.IMetaStorage;
import org.bithon.server.storage.metrics.IMetricStorage;
import org.springframework.context.ApplicationContext;

/**
 * @author Frank Chen
 * @date 23/1/24 2:52 pm
 */
@Slf4j
public class ToMetricStorageExporter implements IMetricExporter {

    private final ApplicationContext applicationContext;
    final MetricMessageHandlers handlers;
    private final SchemaManager schemaManager;

    @JsonCreator
    public ToMetricStorageExporter(@JacksonInject(useInput = OptBoolean.FALSE) ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        this.handlers = MetricMessageHandlers.getInstance();
        this.schemaManager = applicationContext.getBean(SchemaManager.class);
    }

    @Override
    public void process(String messageType, SchemaMetricMessage message) {
        MetricMessageHandler handler = getMessageHandler(messageType, message);
        if (handler == null) {
            log.error("Can't find handler for {}", messageType);
            return;
        }

        handler.process(message.getMetrics());
    }

    private MetricMessageHandler getMessageHandler(String messageType, SchemaMetricMessage message) {
        MetricMessageHandler handler = handlers.getHandler(messageType);
        if (handler != null) {
            // TODO: check if schema is changed
            return handler;
        }

        if (message.getSchema() == null) {
            return null;
        }

        //
        // create a handler
        //
        synchronized (this) {
            handler = handlers.getHandler(messageType);
            if (handler != null) {
                // double check
                return handler;
            }

            schemaManager.addSchema(message.getSchema());
            try {
                handler = new MetricMessageHandler(messageType,
                                                   applicationContext.getBean(IMetaStorage.class),
                                                   applicationContext.getBean(IMetricStorage.class),
                                                   schemaManager,
                                                   null,
                                                   applicationContext.getBean(MetricPipelineConfig.class));

                handlers.add(handler);
                return handler;
            } catch (Exception e) {
                log.error("error", e);
                return null;
            }
        }
    }

    @Override
    public void close() throws Exception {
        for (MetricMessageHandler handler : handlers.getHandlers()) {
            handler.close();
        }
    }

    @Override
    public String toString() {
        return "to-metric-storage";
    }
}
