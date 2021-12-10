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

package org.bithon.server.metric.sink;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.OptBoolean;
import lombok.extern.slf4j.Slf4j;
import org.bithon.server.common.utils.ThreadUtils;
import org.bithon.server.common.utils.collection.CloseableIterator;
import org.bithon.server.common.utils.collection.IteratorableCollection;
import org.springframework.context.ApplicationContext;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * This sink is designed for function evaluation and local development.
 * It calls message handlers directly in process.
 *
 * @author frank.chen021@outlook.com
 * @date 2021/3/15
 */
@Slf4j
@JsonTypeName("local")
public class LocalMetricSink implements IMetricMessageSink {

    private final Map<String, AbstractMetricMessageHandler> handlers = new HashMap<>();
    private final ThreadPoolExecutor executor;

    @JsonCreator
    public LocalMetricSink(@JacksonInject(useInput = OptBoolean.FALSE) ApplicationContext applicationContext) {
        applicationContext.getBeansOfType(AbstractMetricMessageHandler.class).values().forEach(this::add);

        final String name = "metric-sink";
        executor = new ThreadPoolExecutor(2,
                                          32,
                                          1,
                                          TimeUnit.MINUTES,
                                          new LinkedBlockingQueue<>(4096),
                                          new ThreadUtils.NamedThreadFactory(name),
                                          new ThreadPoolExecutor.DiscardPolicy());
        log.info("Starting executor [{}]", name);
    }

    private void add(AbstractMetricMessageHandler handler) {
        handlers.put(handler.getType(), handler);
    }

    @Override
    public void process(String messageType, IteratorableCollection<MetricMessage> messages) {
        AbstractMetricMessageHandler handler = handlers.get(messageType);
        if (handler != null) {
            executor.submit(() -> {
                String oldName = Thread.currentThread().getName();
                Thread.currentThread().setName(oldName + "-" + messageType);
                try {
                    handler.process(messages);
                } catch (Exception e) {
                    log.error("Exception when processing message[{}]: {}", messageType, e);
                } finally {
                    Thread.currentThread().setName(oldName);
                }
            });
        } else {
            log.error("No Handler for message [{}]", messageType);
        }
    }

    @Override
    public void close() throws Exception {
        log.info("Shutting down executor [{}]", "metric-sink");
        executor.shutdown();
    }
}
