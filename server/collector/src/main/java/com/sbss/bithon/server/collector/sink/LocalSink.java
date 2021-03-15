package com.sbss.bithon.server.collector.sink;

import com.sbss.bithon.server.collector.IMessageHandler;
import com.sbss.bithon.server.metric.collector.*;

import java.util.HashMap;
import java.util.Map;

/**
 * This sink is designed for function evaluation and local development.
 * It calls message handlers directly in process.
 *
 * @author frank.chen021@outlook.com
 * @date 2021/3/15
 */
public class LocalSink implements IMessageSink {

    private final Map<String, IMessageHandler> handlers = new HashMap<>();

    public LocalSink(JvmMetricMessageHandler jvmMetricMessageHandler,
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
    public void process(String messageType, Map<String, Object> message) {
        IMessageHandler handler = handlers.get(messageType);
        if (handler != null) {
            //handler.submit();
        }
    }
}
