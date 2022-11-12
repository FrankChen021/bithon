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
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.bithon.component.commons.utils.Preconditions;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.MessageListener;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/18
 */
@Slf4j
public abstract class AbstractKafkaConsumer<MSG> implements IKafkaConsumer, MessageListener<String, String> {
    protected final ObjectMapper objectMapper;
    private final TypeReference<MSG> typeReference;

    ConcurrentMessageListenerContainer<String, String> consumerContainer;

    @Getter
    private String topic;

    public AbstractKafkaConsumer(TypeReference<MSG> typeReference) {
        this.typeReference = typeReference;
        objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    protected abstract void onMessage(String s, MSG msg);

    @Override
    public final void onMessage(ConsumerRecord<String, String> record) {
        Header type = record.headers().lastHeader("type");
        if (type == null) {
            log.error("No header in message from topic: {}", this.topic);
            return;
        }

        try {
            String messageType = new String(type.value(), StandardCharsets.UTF_8);

            onMessage(messageType, objectMapper.readValue(record.value(), typeReference));

        } catch (IOException e) {
            log.error("process message failed", e);
        }
    }

    @Override
    public IKafkaConsumer start(final Map<String, Object> props) {
        Map<String, Object> consumerProperties = new HashMap<>(props);

        topic = (String) props.remove("topic");
        Preconditions.checkNotNull(topic, "topic for [%s] is not configured.", this.getClass().getSimpleName());

        consumerProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        ContainerProperties containerProperties = new ContainerProperties(topic);
        containerProperties.setAckMode(ContainerProperties.AckMode.TIME);
        containerProperties.setAckTime(5000);
        containerProperties.setPollTimeout(1000);
        containerProperties.setGroupId((String) props.getOrDefault(ConsumerConfig.GROUP_ID_CONFIG, "bithon-" + topic));
        containerProperties.setClientId((String) props.getOrDefault(ConsumerConfig.CLIENT_ID_CONFIG, "bithon-" + topic));
        consumerContainer = new ConcurrentMessageListenerContainer<>(new DefaultKafkaConsumerFactory<>(consumerProperties), containerProperties);
        consumerContainer.setupMessageListener(this);
        consumerContainer.start();

        log.info("Starting Kafka consumer for topic [{}]", topic);
        return this;
    }

    @Override
    public void stop() {
        log.info("Stopping Kafka consumer for {}", this.topic);
        if (consumerContainer != null) {
            consumerContainer.stop(true);
        }
    }

    @Override
    public boolean isRunning() {
        return consumerContainer.isRunning();
    }
}
