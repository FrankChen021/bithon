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

package org.bithon.agent.core.metric.domain.mongo;

import org.bithon.agent.core.dispatcher.IMessageConverter;
import org.bithon.agent.core.metric.collector.IntervalMetricCollector;

import java.util.List;

/**
 * @author frankchen
 */
public class MongoDbMetricCollector extends IntervalMetricCollector<MongoDbCompositeMetric> {

    @Override
    protected MongoDbCompositeMetric newMetrics() {
        return new MongoDbCompositeMetric();
    }

    @Override
    protected Object toMessage(IMessageConverter messageConverter,
                               int interval,
                               long timestamp,
                               List<String> dimensions,
                               MongoDbCompositeMetric metric) {
        return messageConverter.from(timestamp, interval, dimensions, metric);
    }

    public MongoDbCompositeMetric getOrCreateMetric(String server, String database) {
        return super.getOrCreateMetric(server, database);
    }

    /**
     * a temp interafce to allow mongodb-3.8 plugin to compile OK
     */
    @Deprecated
    public MongoDbCompositeMetric getOrCreateMetric(String server) {
        return super.getOrCreateMetric(server);
    }
}
