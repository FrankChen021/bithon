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

package org.bithon.server.sink.metrics.exporter;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author frank.chen021@outlook.com
 * @date 2022/7/31 12:17
 */
public class MetricMessageHandlers {

    private static final MetricMessageHandlers INSTANCE = new MetricMessageHandlers();

    public static MetricMessageHandlers getInstance() {
        return INSTANCE;
    }

    private final Map<String, MetricMessageHandler> handlers = new ConcurrentHashMap<>();

    public MetricMessageHandlers() {
    }

    public void add(MetricMessageHandler handler) {
        handlers.put(handler.getType(), handler);
    }

    public MetricMessageHandler remove(String name) {
        return handlers.remove(name);
    }

    public MetricMessageHandler getHandler(String name) {
        return handlers.get(name);
    }

    public Collection<MetricMessageHandler> getHandlers() {
        return handlers.values();
    }
}
