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

package org.bithon.server.collector.sink.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.bithon.server.metric.sink.IMetricMessageSink;
import org.bithon.server.common.utils.collection.CloseableIterator;
import org.bithon.server.metric.sink.MetricMessage;
import org.springframework.kafka.core.KafkaTemplate;

import java.nio.charset.StandardCharsets;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/15
 */
public class KafkaMetricSink implements IMetricMessageSink {

    private final KafkaTemplate<String, String> producer;
    private final ObjectMapper objectMapper;

    public KafkaMetricSink(KafkaTemplate<String, String> producer, ObjectMapper objectMapper) {
        this.producer = producer;
        this.objectMapper = objectMapper;
    }

    @Override
    public void process(String messageType, CloseableIterator<MetricMessage> messages) {
        if (!messages.hasNext()) {
            return;
        }

        String key = null;

        //
        // a batch message in written into a single kafka message in which each text line is a single metric message
        //
        // of course we could also send messages in this batch one by one to Kafka,
        // but I don't think it has advantages over the way below
        //
        StringBuilder messageText = new StringBuilder();
        while (messages.hasNext()) {
            MetricMessage metricMessage = messages.next();

            // Sink receives messages from an agent, it's safe to use instance name of first item
            key = metricMessage.getInstanceName();

            // deserialization
            try {
                messageText.append(objectMapper.writeValueAsString(metricMessage));
            } catch (JsonProcessingException ignored) {
            }

            //it's not necessary, only used to improve readability of text when debugging
            messageText.append('\n');
        }

        ProducerRecord<String, String> record = new ProducerRecord<>("bithon-metrics", key, messageText.toString());
        record.headers().add("type", messageType.getBytes(StandardCharsets.UTF_8));

        this.producer.send(record);
    }
}
