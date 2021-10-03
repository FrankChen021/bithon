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

package com.sbss.bithon.server.collector.sink.local;

import com.sbss.bithon.server.collector.sink.IMessageSink;
import com.sbss.bithon.server.common.handler.IMessageHandler;
import com.sbss.bithon.server.common.utils.collection.CloseableIterator;
import com.sbss.bithon.server.metric.handler.ExceptionMetricMessageHandler;
import com.sbss.bithon.server.metric.handler.HttpIncomingMetricMessageHandler;
import com.sbss.bithon.server.metric.handler.HttpOutgoingMetricMessageHandler;
import com.sbss.bithon.server.metric.handler.JdbcPoolMetricMessageHandler;
import com.sbss.bithon.server.metric.handler.JvmGcMetricMessageHandler;
import com.sbss.bithon.server.metric.handler.JvmMetricMessageHandler;
import com.sbss.bithon.server.metric.handler.MetricMessage;
import com.sbss.bithon.server.metric.handler.MongoDbMetricMessageHandler;
import com.sbss.bithon.server.metric.handler.RedisMetricMessageHandler;
import com.sbss.bithon.server.metric.handler.SqlMetricMessageHandler;
import com.sbss.bithon.server.metric.handler.ThreadPoolMetricMessageHandler;
import com.sbss.bithon.server.metric.handler.WebServerMetricMessageHandler;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * This sink is designed for function evaluation and local development.
 * It calls message handlers directly in process.
 *
 * @author frank.chen021@outlook.com
 * @date 2021/3/15
 */
@Slf4j
public class LocalMetricSink implements IMessageSink<CloseableIterator<MetricMessage>> {

    @Getter
    private final Map<String, IMessageHandler<CloseableIterator<MetricMessage>>> handlers = new HashMap<>();

    public LocalMetricSink(JvmMetricMessageHandler jvmMetricMessageHandler,
                           JvmGcMetricMessageHandler jvmGcMetricMessageHandler,
                           HttpIncomingMetricMessageHandler httpIncomingMetricMessageHandler,
                           WebServerMetricMessageHandler webServerMetricMessageHandler,
                           ExceptionMetricMessageHandler exceptionMetricMessageHandler,
                           HttpOutgoingMetricMessageHandler httpOutgoingMetricMessageHandler,
                           ThreadPoolMetricMessageHandler threadPoolMetricMessageHandler,
                           JdbcPoolMetricMessageHandler jdbcPoolMetricMessageHandler,
                           RedisMetricMessageHandler redisMetricMessageHandler,
                           SqlMetricMessageHandler sqlMetricMessageHandler,
                           MongoDbMetricMessageHandler mongoDbMetricMessageHandler) {
        add(jvmMetricMessageHandler);
        add(jvmGcMetricMessageHandler);
        add(httpIncomingMetricMessageHandler);
        add(webServerMetricMessageHandler);
        add(exceptionMetricMessageHandler);
        add(httpOutgoingMetricMessageHandler);
        add(threadPoolMetricMessageHandler);
        add(jdbcPoolMetricMessageHandler);
        add(redisMetricMessageHandler);
        add(sqlMetricMessageHandler);
        add(mongoDbMetricMessageHandler);
    }

    private void add(IMessageHandler<CloseableIterator<MetricMessage>> handler) {
        handlers.put(handler.getType(), handler);
    }

    @Override
    public void process(String messageType, CloseableIterator<MetricMessage> messages) {
        IMessageHandler<CloseableIterator<MetricMessage>> handler = handlers.get(messageType);
        if (handler != null) {
            handler.submit(messages);
        } else {
            log.error("No Handler for message [{}]", messageType);
        }
    }
}
