/*
 *    Copyright 2020 bithon.cn
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

package com.sbss.bithon.server.collector.kafka;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sbss.bithon.server.common.utils.collection.CloseableIterator;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.BatchMessageListener;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/18
 */
public abstract class AbstractKafkaCollector<MSG> implements IKafkaCollector {
    ConcurrentMessageListenerContainer<String, String> consumerContainer;

    protected final ObjectMapper objectMapper;
    private final Class<MSG> clazz;

    public AbstractKafkaCollector(Class<MSG> clazz) {
        this.clazz = clazz;
        objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    protected abstract String getGroupId();

    protected abstract String getTopic();

    protected abstract void onMessage(String topic, CloseableIterator<MSG> msg);

    protected void onMessage(List<ConsumerRecord<String, String>> records) {
        final Iterator<ConsumerRecord<String, String>> recordIterator = records.iterator();
        CloseableIterator<ConsumerRecord<String, String>> iterator = new CloseableIterator<ConsumerRecord<String, String>>() {
            @Override
            public void close() {
            }

            @Override
            public boolean hasNext() {
                return recordIterator.hasNext();
            }

            @Override
            public ConsumerRecord<String, String> next() {
                return recordIterator.next();
            }
        };
        CloseableIterator<MSG> i = iterator.flatMap((ConsumerRecord<String, String> record) -> {
            try (JsonParser parser = new JsonFactory().createParser(record.value())) {
                final MappingIterator<MSG> delegate = objectMapper.readValues(parser, clazz);
                return new CloseableIterator<MSG>() {
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
                throw new RuntimeException(String.format("Can't parse text into %s.\n%s",
                                                         clazz.getSimpleName(),
                                                         record.value()));
            }
        });

        onMessage(getTopic(), i);
    }

    @Override
    public IKafkaCollector start(Map<String, Object> consumerProps) {

        ContainerProperties containerProperties = new ContainerProperties(getTopic());
        containerProperties.setAckMode(ContainerProperties.AckMode.TIME);
        containerProperties.setAckTime(5000);
        containerProperties.setPollTimeout(1000);
        containerProperties.setGroupId(getGroupId());
        containerProperties.setClientId(getGroupId());
        consumerContainer = new ConcurrentMessageListenerContainer<>(new DefaultKafkaConsumerFactory<>(consumerProps),
                                                                     containerProperties);
        consumerContainer.setupMessageListener(new BatchMessageListener<String, String>() {
            @Override
            public void onMessage(List<ConsumerRecord<String, String>> records) {
                AbstractKafkaCollector.this.onMessage(records);
            }
        });
        consumerContainer.start();

        return this;
    }

    @Override
    public void stop() {
        if (consumerContainer != null) {
            consumerContainer.stop(true);
        }
    }

    @Override
    public boolean isRunning() {
        return consumerContainer.isRunning();
    }
}
