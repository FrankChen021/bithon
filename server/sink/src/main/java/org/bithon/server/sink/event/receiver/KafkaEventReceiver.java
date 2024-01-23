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

package org.bithon.server.sink.event.receiver;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.bithon.server.kafka.AbstractKafkaConsumer;
import org.bithon.server.sink.event.IEventProcessor;
import org.bithon.server.storage.event.EventMessage;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/18
 */
@Slf4j
public class KafkaEventReceiver extends AbstractKafkaConsumer implements IEventReceiver {
    private IEventProcessor processor;
    private final TypeReference<List<EventMessage>> typeReference;
    private final Map<String, Object> props;

    @JsonCreator
    public KafkaEventReceiver(@JacksonInject(useInput = OptBoolean.FALSE) Map<String, Object> props,
                              @JacksonInject(useInput = OptBoolean.FALSE) ApplicationContext applicationContext) {
        super(applicationContext);

        this.props = props;
        this.typeReference = new TypeReference<List<EventMessage>>() {
        };
    }

    @Override
    public void start() {
        start(props);
    }

    @Override
    public void registerProcessor(IEventProcessor processor) {
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
        List<EventMessage> events = new ArrayList<>(16);

        for (ConsumerRecord<String, byte[]> record : records) {
            try {
                List<EventMessage> msg = objectMapper.readValue(record.value(), typeReference);
                events.addAll(msg);
            } catch (IOException e) {
                log.error("Failed to process message [event] failed", e);
            }
        }

        processor.process(getTopic(), events);
    }
}
