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

package org.bithon.server.pipeline.event.exporter;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.bithon.component.commons.utils.Preconditions;
import org.bithon.server.storage.event.EventMessage;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/15
 */
@Slf4j
public class KafkaEventExporter implements IEventExporter {

    private final KafkaTemplate<String, String> producer;
    private final ObjectMapper objectMapper;
    private final String topic;

    @JsonCreator
    public KafkaEventExporter(@JsonProperty("props") Map<String, Object> props,
                              @JacksonInject(useInput = OptBoolean.FALSE) ObjectMapper objectMapper) {
        this.topic = (String) props.remove("topic");
        Preconditions.checkNotNull(topic, "topic is not configured for event sink");

        this.producer = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(props,
                                                                              new StringSerializer(),
                                                                              new StringSerializer()),
                                            ImmutableMap.of(ProducerConfig.CLIENT_ID_CONFIG, "event"));
        this.objectMapper = objectMapper;
    }

    @Override
    public void process(String messageType, List<EventMessage> messages) {
        String key = null;

        StringBuilder messageText = new StringBuilder(512);
        messageText.append('[');
        for (EventMessage eventMessage : messages) {

            // Sink receives messages from an agent, it's safe to use instance name of first item
            if (key == null) {
                key = messageType + "/" + eventMessage.getAppName() + "/" + eventMessage.getInstanceName();
            }

            // deserialization
            try {
                messageText.append(objectMapper.writeValueAsString(eventMessage));
            } catch (JsonProcessingException ignored) {
            }

            messageText.append(",\n");
        }
        if (messageText.length() > 2) {
            // Remove last separator
            messageText.delete(messageText.length() - 2, messageText.length());
        }
        messageText.append(']');
        if (key != null) {
            ProducerRecord<String, String> record = new ProducerRecord<>(this.topic, key, messageText.toString());
            producer.send(record);
        }
    }

    @Override
    public void close() {
        producer.destroy();
    }
}
