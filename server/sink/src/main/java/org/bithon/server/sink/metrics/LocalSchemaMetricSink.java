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

package org.bithon.server.sink.metrics;

import lombok.extern.slf4j.Slf4j;
import org.bithon.server.storage.datasource.DataSourceSchemaManager;
import org.bithon.server.storage.meta.IMetaStorage;
import org.bithon.server.storage.metrics.IMetricStorage;
import org.springframework.context.ApplicationContext;

/**
 * @author frank.chen021@outlook.com
 * @date 3/10/21 14:11
 */
@Slf4j
public class LocalSchemaMetricSink implements IMessageSink<SchemaMetricMessage> {

    final MetricMessageHandlers handlers;
    final DataSourceSchemaManager schemaManager;
    final ApplicationContext applicationContext;

    public LocalSchemaMetricSink(ApplicationContext applicationContext) {
        this.schemaManager = applicationContext.getBean(DataSourceSchemaManager.class);
        this.handlers = applicationContext.getBean(MetricMessageHandlers.class);
        this.applicationContext = applicationContext;
    }

    @Override
    public void process(String messageType, SchemaMetricMessage message) {
        AbstractMetricMessageHandler handler = getMessageHandler(message);
        if (handler == null) {
            log.error("Can't find handler for {}", message.getSchema().getName());
            return;
        }

        handler.process(message.getMetrics());
    }

    private AbstractMetricMessageHandler getMessageHandler(SchemaMetricMessage message) {
        AbstractMetricMessageHandler handler = handlers.getHandler(message.getSchema().getName());
        if (handler != null) {
            // TODO: check if schema is changed
            return handler;
        }
        //
        // create  a handler
        //
        synchronized (this) {
            handler = handlers.getHandler(message.getSchema().getName());
            if (handler != null) {
                // double check
                return handler;
            }

            schemaManager.addDataSourceSchema(message.getSchema());
            try {
                handler = new MetricMessageHandler(message.getSchema().getName(),
                                                   applicationContext.getBean(IMetaStorage.class),
                                                   applicationContext.getBean(IMetricStorage.class),
                                                   schemaManager);

                handlers.add(handler);
                return handler;
            } catch (Exception e) {
                log.error("error", e);
                return null;
            }
        }
    }
}
