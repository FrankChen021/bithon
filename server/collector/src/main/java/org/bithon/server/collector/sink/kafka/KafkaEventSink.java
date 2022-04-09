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
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.bithon.component.commons.collection.IteratorableCollection;
import org.bithon.server.sink.event.IEventMessageSink;
import org.bithon.server.storage.event.EventMessage;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/15
 */
@Slf4j
@JsonTypeName("kafka")
public class KafkaEventSink implements IEventMessageSink {

    private final KafkaTemplate<String, String> producer;
    private final ObjectMapper objectMapper;

    @JsonCreator
    public KafkaEventSink(@JsonProperty("props") Map<String, Object> props,
                          @JacksonInject(useInput = OptBoolean.FALSE) ObjectMapper objectMapper) {
        this.producer = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(props,
                                                                              new StringSerializer(),
                                                                              new StringSerializer()),
                                            ImmutableMap.of("client.id", "event"));
        this.objectMapper = objectMapper;
    }

    @Override
    public void process(String messageType, IteratorableCollection<EventMessage> messages) {
        String key = null;
        StringBuilder messageText = new StringBuilder();
        while (messages.hasNext()) {
            EventMessage eventMessage = messages.next();

            // Sink receives messages from an agent, it's safe to use instance name of first item
            key = eventMessage.getInstanceName();

            // deserialization
            try {
                messageText.append(objectMapper.writeValueAsString(eventMessage));
            } catch (JsonProcessingException ignored) {
            }

            //it's not necessary, only used to improve readability of text when debugging
            messageText.append('\n');
        }
        if (key != null) {
            ProducerRecord<String, String> record = new ProducerRecord<>("bithon-event", key, messageText.toString());
            record.headers().add("type", messageType.getBytes(StandardCharsets.UTF_8));
            producer.send(record);
        }
    }

    @Override
    public void close() throws Exception {
        producer.destroy();
    }
}
