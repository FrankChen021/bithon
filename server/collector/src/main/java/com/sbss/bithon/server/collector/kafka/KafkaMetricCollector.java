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
import com.sbss.bithon.server.collector.sink.local.LocalMetricSink;
import com.sbss.bithon.server.common.utils.collection.SizedIterator;
import com.sbss.bithon.server.metric.handler.ExceptionMetricMessageHandler;
import com.sbss.bithon.server.metric.handler.GenericMetricMessage;
import com.sbss.bithon.server.metric.handler.HttpClientMetricMessageHandler;
import com.sbss.bithon.server.metric.handler.JdbcPoolMetricMessageHandler;
import com.sbss.bithon.server.metric.handler.JvmGcMetricMessageHandler;
import com.sbss.bithon.server.metric.handler.JvmMetricMessageHandler;
import com.sbss.bithon.server.metric.handler.MongoDbMetricMessageHandler;
import com.sbss.bithon.server.metric.handler.RedisMetricMessageHandler;
import com.sbss.bithon.server.metric.handler.SqlMetricMessageHandler;
import com.sbss.bithon.server.metric.handler.ThreadPoolMetricMessageHandler;
import com.sbss.bithon.server.metric.handler.WebRequestMetricMessageHandler;
import com.sbss.bithon.server.metric.handler.WebServerMetricMessageHandler;

/**
 * Kafka collector that is connecting to {@link com.sbss.bithon.server.collector.sink.kafka.KafkaMetricSink}
 *
 * @author frank.chen021@outlook.com
 * @date 2021/3/18
 */
public class KafkaMetricCollector extends AbstractKafkaCollector<SizedIterator<GenericMetricMessage>> {

    private final LocalMetricSink localSink;

    public KafkaMetricCollector(JvmMetricMessageHandler jvmMetricMessageHandler,
                                JvmGcMetricMessageHandler jvmGcMetricMessageHandler,
                                WebRequestMetricMessageHandler webRequestMetricMessageHandler,
                                WebServerMetricMessageHandler webServerMetricMessageHandler,
                                ExceptionMetricMessageHandler exceptionMetricMessageHandler,
                                HttpClientMetricMessageHandler httpClientMetricMessageHandler,
                                ThreadPoolMetricMessageHandler threadPoolMetricMessageHandler,
                                JdbcPoolMetricMessageHandler jdbcPoolMetricMessageHandler,
                                RedisMetricMessageHandler redisMetricMessageHandler,
                                SqlMetricMessageHandler sqlMetricMessageHandler,
                                MongoDbMetricMessageHandler mongoDbMetricMessageHandler) {
        super(null);
        localSink = new LocalMetricSink(jvmMetricMessageHandler,
                                        jvmGcMetricMessageHandler,
                                        webRequestMetricMessageHandler,
                                        webServerMetricMessageHandler,
                                        exceptionMetricMessageHandler,
                                        httpClientMetricMessageHandler,
                                        threadPoolMetricMessageHandler,
                                        jdbcPoolMetricMessageHandler,
                                        redisMetricMessageHandler,
                                        sqlMetricMessageHandler,
                                        mongoDbMetricMessageHandler);
    }

    @Override
    protected String getGroupId() {
        return "bithon-collector-metric";
    }

    @Override
    protected String[] getTopics() {
        return this.localSink.getHandlers().keySet().toArray(new String[0]);
    }

    @Override
    protected void onMessage(String topic, String rawMessage) {
        try {
            GenericMetricMessage[] messages = objectMapper.readValue(rawMessage, GenericMetricMessage[].class);

            localSink.process(topic, new SizedIterator<GenericMetricMessage>() {
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
