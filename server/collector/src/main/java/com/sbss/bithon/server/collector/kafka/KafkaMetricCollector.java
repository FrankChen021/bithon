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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sbss.bithon.server.common.utils.collection.SizedIterator;
import com.sbss.bithon.server.metric.handler.AbstractMetricMessageHandler;
import com.sbss.bithon.server.metric.handler.GenericMetricMessage;

/**
 * Kafka collector that is connecting to {@link com.sbss.bithon.server.collector.sink.kafka.KafkaMetricSink}
 *
 * @author frank.chen021@outlook.com
 * @date 2021/3/18
 */
public class KafkaMetricCollector extends AbstractKafkaCollector<SizedIterator<GenericMetricMessage>> {

    private final AbstractMetricMessageHandler messageHandler;

    public KafkaMetricCollector(AbstractMetricMessageHandler messageHandler) {
        super(null);
        this.messageHandler = messageHandler;
    }

    @Override
    protected String getGroupId() {
        return "bithon-collector-consumer-" + this.messageHandler.getType();
    }

    @Override
    protected String getTopic() {
        return this.messageHandler.getType();
    }

    @Override
    protected void onMessage(String topic, String rawMessage) {
        try {
            GenericMetricMessage[] messages = objectMapper.readValue(rawMessage, GenericMetricMessage[].class);

            messageHandler.submit(new SizedIterator<GenericMetricMessage>() {
                int index = 0;

                @Override
                public int size() {
                    return messages.length;
                }

                @Override
                public void close() {
                }

                @Override
                public boolean hasNext() {
                    return index < messages.length;
                }

                @Override
                public GenericMetricMessage next() {
                    return messages[index++];
                }
            });
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onMessage(String topic, SizedIterator<GenericMetricMessage> metric) {
    }
}
