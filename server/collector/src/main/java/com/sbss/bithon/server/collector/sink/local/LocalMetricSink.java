package com.sbss.bithon.server.collector.sink.local;

import com.sbss.bithon.server.collector.sink.IMessageSink;
import com.sbss.bithon.server.common.handler.IMessageHandler;
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
public class LocalMetricSink implements IMessageSink<SizedIterator<GenericMetricMessage>> {

    @Getter
    private final Map<String, IMessageHandler<SizedIterator<GenericMetricMessage>>> handlers = new HashMap<>();

    public LocalMetricSink(JvmMetricMessageHandler jvmMetricMessageHandler,
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
        add(jvmMetricMessageHandler);
        add(jvmGcMetricMessageHandler);
        add(webRequestMetricMessageHandler);
        add(webServerMetricMessageHandler);
        add(exceptionMetricMessageHandler);
        add(httpClientMetricMessageHandler);
        add(threadPoolMetricMessageHandler);
        add(jdbcPoolMetricMessageHandler);
        add(redisMetricMessageHandler);
        add(sqlMetricMessageHandler);
        add(mongoDbMetricMessageHandler);
    }

    private void add(IMessageHandler<SizedIterator<GenericMetricMessage>> handler) {
        handlers.put(handler.getType(), handler);
    }

    @Override
    public void process(String messageType, SizedIterator<GenericMetricMessage> messages) {
        IMessageHandler<SizedIterator<GenericMetricMessage>> handler = handlers.get(messageType);
        if (handler != null) {
            handler.submit(messages);
        } else {
            log.error("No Handler for message [{}]", messageType);
        }
    }
}
