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

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author frank.chen
 * @date 2022/7/31 12:17
 */
@Component
public class MetricMessageHandlers {

    private final Map<String, AbstractMetricMessageHandler> handlers = new ConcurrentHashMap<>();

    public MetricMessageHandlers(ApplicationContext applicationContext) {

        Class<? extends AbstractMetricMessageHandler>[] handlers = new Class[]{
            ExceptionMetricMessageHandler.class,
            HttpIncomingMetricMessageHandler.class,
            HttpOutgoingMetricMessageHandler.class,
            JdbcPoolMetricMessageHandler.class,
            JvmMetricMessageHandler.class,
            JvmGcMetricMessageHandler.class,
            MongoDbMetricMessageHandler.class,
            RedisMetricMessageHandler.class,
            SqlMetricMessageHandler.class,
            ThreadPoolMetricMessageHandler.class,
            WebServerMetricMessageHandler.class
        };
        for (Class<? extends AbstractMetricMessageHandler> handlerClass : handlers) {
            this.add(applicationContext.getAutowireCapableBeanFactory().createBean(handlerClass));
        }
    }

    public void add(AbstractMetricMessageHandler handler) {
        handlers.put(handler.getType(), handler);
    }

    public void remove(String name) {
        handlers.remove(name);
    }

    public AbstractMetricMessageHandler getHandler(String name) {
        return handlers.get(name);
    }

    public Collection<AbstractMetricMessageHandler> getHandlers() {
        return handlers.values();
    }
}
