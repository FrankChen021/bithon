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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.bithon.component.commons.collection.CloseableIterator;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.MessageListener;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/18
 */
@Slf4j
public abstract class AbstractKafkaConsumer<MSG> implements IKafkaConsumer, MessageListener<String, String> {
    protected final ObjectMapper objectMapper;
    private final Class<MSG> clazz;
    ConcurrentMessageListenerContainer<String, String> consumerContainer;

    public AbstractKafkaConsumer(Class<MSG> clazz) {
        this.clazz = clazz;
        objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    protected abstract String getGroupId();

    protected abstract String getTopic();

    protected abstract void onMessage(String s, CloseableIterator<MSG> msg);

    @Override
    public final void onMessage(ConsumerRecord<String, String> record) {
        CloseableIterator<MSG> iterator;
        try {
            final JsonParser parser = new JsonFactory().createParser(record.value());
            final MappingIterator<MSG> delegate = objectMapper.readValues(parser, clazz);
            iterator = new CloseableIterator<MSG>() {
                @Override
                public boolean hasNext() {
                    return delegate.hasNext();
                }

                @Override
                public MSG next() {
                    return delegate.next();
                }

                @Override
                public void close() throws IOException {
                    delegate.close();
                }
            };
        } catch (IOException e) {
            throw new RuntimeException(String.format(Locale.ENGLISH,
                                                     "Can't parse text into %s.%n%s",
                                                     clazz.getSimpleName(),
                                                     record.value()));
        }

        Header type = record.headers().lastHeader("type");
        if (type != null) {
            onMessage(new String(type.value(), StandardCharsets.UTF_8), iterator);
        } else {
            log.error("No header in message from topic: {}", this.getTopic());
        }
        try {
            iterator.close();
        } catch (IOException ignored) {
        }
    }

    @Override
    public IKafkaConsumer start(Map<String, Object> consumerProps) {

        ContainerProperties containerProperties = new ContainerProperties(getTopic());
        containerProperties.setAckMode(ContainerProperties.AckMode.TIME);
        containerProperties.setAckTime(5000);
        containerProperties.setPollTimeout(1000);
        containerProperties.setGroupId(getGroupId());
        containerProperties.setClientId(getGroupId());
        consumerContainer = new ConcurrentMessageListenerContainer<>(new DefaultKafkaConsumerFactory<>(consumerProps),
                                                                     containerProperties);
        consumerContainer.setupMessageListener(this);
        consumerContainer.start();

        return this;
    }

    @Override
    public void stop() {
        log.info("Stopping Kafka consumer for {}", getTopic());
        if (consumerContainer != null) {
            consumerContainer.stop(true);
        }
    }

    @Override
    public boolean isRunning() {
        return consumerContainer.isRunning();
    }
}
