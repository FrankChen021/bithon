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
import org.bithon.component.commons.utils.NumberUtils;
import org.bithon.component.commons.utils.Preconditions;
import org.bithon.component.commons.utils.StringUtils;
import org.springframework.context.ApplicationContext;
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
    private final ApplicationContext applicationContext;

    private ConcurrentMessageListenerContainer<String, String> consumerContainer;

    @Getter
    private String topic;

    public AbstractKafkaConsumer(TypeReference<MSG> typeReference, ApplicationContext applicationContext) {
        this.typeReference = typeReference;
        this.applicationContext = applicationContext;
        this.objectMapper = new ObjectMapper();
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
        String messageType = new String(type.value(), StandardCharsets.UTF_8);
        try {
            onMessage(messageType, objectMapper.readValue(record.value(), typeReference));
        } catch (IOException e) {
            log.error(StringUtils.format("Failed to process message [%s] failed", messageType), e);
        }
    }

    @Override
    public IKafkaConsumer start(final Map<String, Object> props) {
        int pollTimeout = NumberUtils.getInteger(props.remove("pollTimeout"), 1000);
        Preconditions.checkIf(pollTimeout >= 100, "'pollTimeout' must be >= 100, given value is %d", pollTimeout);

        int ackTime = NumberUtils.getInteger(props.remove("ackTime"), 5000);
        Preconditions.checkIf(ackTime >= 100 && ackTime <= 60_000, "'ackTime' must be >= 100 && <= 60_000, given value is %d", ackTime);

        int concurrency = NumberUtils.getInteger(props.remove("concurrency"), 1);
        Preconditions.checkIf(concurrency > 0 && concurrency <= 64, "'concurrency' must be > 0 and <= 64, given values is: %d", concurrency);

        topic = (String) props.remove("topic");
        Preconditions.checkNotNull(topic, "topic for [%s] is not configured.", this.getClass().getSimpleName());

        Map<String, Object> consumerProperties = new HashMap<>(props);
        consumerProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProperties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        ContainerProperties containerProperties = new ContainerProperties(topic);
        containerProperties.setAckMode(ContainerProperties.AckMode.TIME);
        containerProperties.setAckTime(ackTime);
        containerProperties.setPollTimeout(pollTimeout);
        containerProperties.setGroupId((String) props.getOrDefault(ConsumerConfig.GROUP_ID_CONFIG, "bithon-" + topic));
        containerProperties.setClientId((String) props.getOrDefault(ConsumerConfig.CLIENT_ID_CONFIG, "bithon-" + topic));
        consumerContainer = new ConcurrentMessageListenerContainer<>(new DefaultKafkaConsumerFactory<>(consumerProperties), containerProperties);
        consumerContainer.setupMessageListener(this);
        consumerContainer.setConcurrency(concurrency);
        consumerContainer.setApplicationEventPublisher(applicationContext);
        consumerContainer.setApplicationContext(applicationContext);
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
