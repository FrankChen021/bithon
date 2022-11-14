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

import com.fasterxml.jackson.core.type.TypeReference;
import org.bithon.server.sink.metrics.LocalMetricSink;
import org.bithon.server.sink.metrics.SchemaMetricMessage;
import org.springframework.context.ApplicationContext;

/**
 * Kafka collector that is connecting to KafkaMetricSink
 *
 * @author frank.chen021@outlook.com
 * @date 2021/3/18
 */
public class KafkaMetricConsumer extends AbstractKafkaConsumer<SchemaMetricMessage> {

    private final LocalMetricSink metricSink;

    public KafkaMetricConsumer(LocalMetricSink metricSink, ApplicationContext applicationContext) {
        super(new TypeReference<SchemaMetricMessage>() {
        }, applicationContext);
        this.metricSink = metricSink;
    }

    @Override
    protected void onMessage(String type, SchemaMetricMessage msg) {
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
