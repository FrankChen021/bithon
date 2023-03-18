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

package org.bithon.agent.observability.metric.domain.redis;

import org.bithon.agent.observability.metric.collector.MetricRegistry;
import org.bithon.agent.observability.metric.collector.MetricRegistryFactory;

import java.util.Arrays;

/**
 * @author frankchen
 */
public class RedisMetricRegistry extends MetricRegistry<RedisClientMetrics> {

    public static RedisMetricRegistry get() {
        return MetricRegistryFactory.getOrCreateRegistry("redis-metrics", RedisMetricRegistry::new);
    }

    private RedisMetricRegistry() {
        super("redis-metrics",
              Arrays.asList("uri", "command"),
              RedisClientMetrics.class,
              RedisClientMetrics::new,
              true);
    }

    public RedisClientMetrics getOrCreateMetrics(String uri, String command) {
        return super.getOrCreateMetrics(uri, command);
    }
}
