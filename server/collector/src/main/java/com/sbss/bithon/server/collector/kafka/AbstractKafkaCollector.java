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
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.MessageListener;

import java.io.IOException;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/18
 */
public abstract class AbstractKafkaCollector<MSG> implements IKafkaCollector, MessageListener<String, String> {
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

    protected abstract void onMessage(CloseableIterator<MSG> msg);

    @Override
    public final void onMessage(ConsumerRecord<String, String> record) {
        CloseableIterator<MSG> metricIterator;
        try (JsonParser parser = new JsonFactory().createParser(record.value())) {
            final MappingIterator<MSG> delegate = objectMapper.readValues(parser, clazz);
            metricIterator = new CloseableIterator<MSG>() {
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

        onMessage(metricIterator);
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
        consumerContainer.setupMessageListener(this);
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
