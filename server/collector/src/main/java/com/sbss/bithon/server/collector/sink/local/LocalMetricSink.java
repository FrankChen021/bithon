package com.sbss.bithon.server.collector.sink.local;

import com.sbss.bithon.server.collector.sink.IMessageSink;
import com.sbss.bithon.server.common.handler.IMessageHandler;
import com.sbss.bithon.server.metric.handler.ExceptionMetricMessageHandler;
import com.sbss.bithon.server.metric.handler.GenericMetricMessage;
import com.sbss.bithon.server.metric.handler.HttpClientMetricMessageHandler;
import com.sbss.bithon.server.metric.handler.JdbcPoolMetricMessageHandler;
import com.sbss.bithon.server.metric.handler.JvmGcMetricMessageHandler;
import com.sbss.bithon.server.metric.handler.JvmMetricMessageHandler;
import com.sbss.bithon.server.metric.handler.RedisMetricMessageHandler;
import com.sbss.bithon.server.metric.handler.ThreadPoolMetricMessageHandler;
import com.sbss.bithon.server.metric.handler.WebRequestMetricMessageHandler;
import com.sbss.bithon.server.metric.handler.WebServerMetricMessageHandler;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * This sink is designed for function evaluation and local development.
 * It calls message handlers directly in process.
 *
 * @author frank.chen021@outlook.com
 * @date 2021/3/15
 */
public class LocalMetricSink implements IMessageSink<GenericMetricMessage> {

    @Getter
    private final Map<String, IMessageHandler> handlers = new HashMap<>();

    public LocalMetricSink(JvmMetricMessageHandler jvmMetricMessageHandler,
                           JvmGcMetricMessageHandler jvmGcMetricMessageHandler,
                           WebRequestMetricMessageHandler webRequestMetricMessageHandler,
                           WebServerMetricMessageHandler webServerMetricMessageHandler,
                           ExceptionMetricMessageHandler exceptionMetricMessageHandler,
                           HttpClientMetricMessageHandler httpClientMetricMessageHandler,
                           ThreadPoolMetricMessageHandler threadPoolMetricMessageHandler,
                           JdbcPoolMetricMessageHandler jdbcPoolMetricMessageHandler,
                           RedisMetricMessageHandler redisMetricMessageHandler) {
        add(jvmMetricMessageHandler);
        add(jvmGcMetricMessageHandler);
        add(webRequestMetricMessageHandler);
        add(webServerMetricMessageHandler);
        add(exceptionMetricMessageHandler);
        add(httpClientMetricMessageHandler);
        add(threadPoolMetricMessageHandler);
        add(jdbcPoolMetricMessageHandler);
        add(redisMetricMessageHandler);
    }

    private void add(IMessageHandler handler) {
        handlers.put(handler.getType(), handler);
    }

    @Override
    public void process(String messageType, GenericMetricMessage message) {
        IMessageHandler handler = handlers.get(messageType);
        if (handler != null) {
            handler.submit(message);
        }
    }
}
