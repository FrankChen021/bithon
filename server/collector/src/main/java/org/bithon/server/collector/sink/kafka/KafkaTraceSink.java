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

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.bithon.component.commons.utils.Preconditions;
import org.bithon.server.sink.tracing.ITraceMessageSink;
import org.bithon.server.storage.tracing.TraceSpan;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/15
 */
@JsonTypeName("kafka")
public class KafkaTraceSink implements ITraceMessageSink {

    private final KafkaTemplate<String, String> producer;
    private final ObjectMapper objectMapper;
    private final String topic;

    @JsonCreator
    public KafkaTraceSink(@JsonProperty("props") Map<String, Object> props,
                          @JacksonInject(useInput = OptBoolean.FALSE) ObjectMapper objectMapper) {
        this.topic = (String) props.remove("topic");
        Preconditions.checkNotNull(topic, "topic is not configured for tracing sink");

        this.producer = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(props,
                                                                              new StringSerializer(),
                                                                              new StringSerializer()),
                                            ImmutableMap.of(ProducerConfig.CLIENT_ID_CONFIG, "trace"));
        this.objectMapper = objectMapper;
    }

    @Override
    public void process(String messageType, List<TraceSpan> spans) {
        if (spans.isEmpty()) {
            return;
        }

        String key = null;

        //
        // a batch message in written into a single kafka message in which each text line is a single metric message
        //
        // of course we could also send messages in this batch one by one to Kafka,
        // but I don't think it has advantages over the way below
        //
        StringBuilder messageText = new StringBuilder(2048);
        messageText.append('[');
        for (TraceSpan span : spans) {
            if (key == null) {
                key = span.getTraceId();
            }

            try {
                messageText.append(objectMapper.writeValueAsString(span));
            } catch (JsonProcessingException ignored) {
            }

            messageText.append(",\n");
        }
        if (messageText.length() > 2) {
            // Remove last separator
            messageText.delete(messageText.length() - 2, messageText.length());
        }

        messageText.append(']');

        ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, messageText.toString());
        record.headers().add("type", messageType.getBytes(StandardCharsets.UTF_8));
        producer.send(record);
    }

    @Override
    public void close() {
        producer.destroy();
    }
}
