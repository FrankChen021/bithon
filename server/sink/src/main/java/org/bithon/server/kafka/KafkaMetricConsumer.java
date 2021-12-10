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

package org.bithon.server.kafka;

import org.bithon.server.common.utils.collection.CloseableIterator;
import org.bithon.server.common.utils.collection.IteratorableCollection;
import org.bithon.server.metric.sink.LocalMetricSink;
import org.bithon.server.metric.sink.MetricMessage;

/**
 * Kafka collector that is connecting to KafkaMetricSink
 *
 * @author frank.chen021@outlook.com
 * @date 2021/3/18
 */
public class KafkaMetricConsumer extends AbstractKafkaConsumer<MetricMessage> {

    private final LocalMetricSink metricSink;

    public KafkaMetricConsumer(LocalMetricSink metricSink) {
        super(MetricMessage.class);
        this.metricSink = metricSink;
    }

    @Override
    protected String getGroupId() {
        return "bithon-metric-consumer";
    }

    @Override
    protected String getTopic() {
        return "bithon-metrics";
    }

    @Override
    protected void onMessage(String type, IteratorableCollection<MetricMessage> msg) {
        metricSink.process(type, msg);
    }

    @Override
    public void stop() {
        // stop receiving
        try {
            super.stop();
        } catch (Exception ignored) {
        }

        try {
            metricSink.close();
        } catch (Exception ignored) {
        }
    }
}
