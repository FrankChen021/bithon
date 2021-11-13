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

package org.bithon.server.collector.sink.local;

import lombok.extern.slf4j.Slf4j;
import org.bithon.server.collector.sink.IMessageSink;
import org.bithon.server.common.utils.ThreadUtils;
import org.bithon.server.meta.storage.IMetaStorage;
import org.bithon.server.metric.DataSourceSchemaManager;
import org.bithon.server.metric.handler.AbstractMetricMessageHandler;
import org.bithon.server.metric.handler.SchemaMetricMessage;
import org.bithon.server.metric.storage.IMetricStorage;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author Frank Chen
 * @date 3/10/21 14:11
 */
@Slf4j
public class LocalSchemaMetricSink implements IMessageSink<SchemaMetricMessage> {

    final Map<String, AbstractMetricMessageHandler> handlers;
    final DataSourceSchemaManager schemaManager;
    final ApplicationContext applicationContext;
    final ThreadPoolExecutor executor;

    public LocalSchemaMetricSink(ApplicationContext applicationContext) {
        Map<String, AbstractMetricMessageHandler> handlers = applicationContext.getBeansOfType(AbstractMetricMessageHandler.class);

        // load pre-defined handlers
        this.handlers = new ConcurrentHashMap<>(handlers.values()
                                                        .stream()
                                                        .collect(Collectors.toMap(key -> key.getType(),
                                                                                  v -> v)));

        this.schemaManager = applicationContext.getBean(DataSourceSchemaManager.class);
        this.applicationContext = applicationContext;

        final String name = "schema-metric-sink";
        executor = new ThreadPoolExecutor(2,
                                          32,
                                          1,
                                          TimeUnit.MINUTES,
                                          new LinkedBlockingQueue<>(4096),
                                          new ThreadUtils.NamedThreadFactory(name),
                                          new ThreadPoolExecutor.DiscardPolicy());
        log.info("Starting executor [{}]", name);

        Thread shutdownThread = new Thread(() -> {
            log.info("Shutting down executor [{}]", name);
            executor.shutdown();
        });
        shutdownThread.setName(name + "-shutdown");
        Runtime.getRuntime().addShutdownHook(shutdownThread);
    }

    @Override
    public void process(String messageType, SchemaMetricMessage message) {
        AbstractMetricMessageHandler handler = getMessageHandler(message);
        if (handler == null) {
            log.error("Can't find handler for {}", message.getSchema().getName());
            return;
        }

        executor.submit(() -> {
            String oldName = Thread.currentThread().getName();
            Thread.currentThread().setName(oldName + "-" + messageType);
            try {
                handler.process(message.getMetrics());
            } finally {
                Thread.currentThread().setName(oldName);
            }
        });

    }

    private AbstractMetricMessageHandler getMessageHandler(SchemaMetricMessage message) {
        AbstractMetricMessageHandler handler = handlers.get(message.getSchema().getName());
        if (handler == null) {
            //
            // create  a handler
            //
            synchronized (this) {
                handler = handlers.get(message.getSchema().getName());
                if (handler != null) {
                    return handler;
                }
                schemaManager.addDataSourceSchema(message.getSchema());
                try {
                    handler = new MetricMessageHandler(message.getSchema().getName(),
                                                       applicationContext.getBean(IMetaStorage.class),
                                                       applicationContext.getBean(IMetricStorage.class),
                                                       schemaManager);

                    handlers.put(message.getSchema().getName(), handler);
                    return handler;
                } catch (IOException e) {
                    log.error("error", e);
                    return null;
                }
            }
        } else {
            // TODO: check if schema is changed
            return handler;
        }
    }

    static class MetricMessageHandler extends AbstractMetricMessageHandler {

        public MetricMessageHandler(String dataSourceName,
                                    IMetaStorage metaStorage,
                                    IMetricStorage metricStorage,
                                    DataSourceSchemaManager dataSourceSchemaManager) throws IOException {
            super(dataSourceName,
                  metaStorage,
                  metricStorage,
                  dataSourceSchemaManager);
        }
    }
}
