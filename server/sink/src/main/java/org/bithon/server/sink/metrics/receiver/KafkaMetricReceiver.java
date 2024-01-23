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

package org.bithon.server.sink.metrics.receiver;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.bithon.server.kafka.AbstractKafkaConsumer;
import org.bithon.server.sink.metrics.IMetricProcessor;
import org.bithon.server.sink.metrics.SchemaMetricMessage;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Kafka collector that is connecting to KafkaMetricSink
 *
 * @author frank.chen021@outlook.com
 * @date 2021/3/18
 */
@Slf4j
public class KafkaMetricReceiver extends AbstractKafkaConsumer implements IMetricReceiver {

    private IMetricProcessor processor;
    private final TypeReference<SchemaMetricMessage> typeReference;
    private final Map<String, Object> props;

    @JsonCreator
    public KafkaMetricReceiver(@JacksonInject(useInput = OptBoolean.FALSE) Map<String, Object> props,
                               @JacksonInject(useInput = OptBoolean.FALSE) ApplicationContext applicationContext) {
        super(applicationContext);
        this.typeReference = new TypeReference<SchemaMetricMessage>() {
        };
        this.props = props;
    }

    @Override
    public void start() {
        start(props);
    }

    @Override
    public void registerProcessor(IMetricProcessor processor) {
        this.processor = processor;
    }

    @Override
    public void stop() {
        // stop receiving
        try {
            super.stop();
        } catch (Exception ignored) {
        }

        try {
            processor.close();
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onMessage(List<ConsumerRecord<String, byte[]>> records) {
        Map<String, SchemaMetricMessage> messages = new HashMap<>();

        for (ConsumerRecord<String, byte[]> record : records) {
            Header type = record.headers().lastHeader("type");
            if (type == null) {
                log.error("No header in message from topic: {}", this.getTopic());
                return;
            }
            String messageType = new String(type.value(), StandardCharsets.UTF_8);

            try {
                SchemaMetricMessage msg = objectMapper.readValue(record.value(), typeReference);
                messages.computeIfAbsent(messageType, (v) -> new SchemaMetricMessage(msg.getSchema(), new ArrayList<>()))
                        .getMetrics().addAll(msg.getMetrics());
            } catch (IOException e) {
                log.error("Failed to process message [event] failed", e);
            }
        }

        messages.forEach(processor::process);
    }
}
