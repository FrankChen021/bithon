package com.sbss.bithon.server.collector.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.sbss.bithon.server.collector.sink.local.LocalMetricSink;
import com.sbss.bithon.server.metric.handler.ExceptionMetricMessageHandler;
import com.sbss.bithon.server.metric.handler.GenericMetricMessage;
import com.sbss.bithon.server.metric.handler.HttpClientMetricMessageHandler;
import com.sbss.bithon.server.metric.handler.JdbcPoolMetricMessageHandler;
import com.sbss.bithon.server.metric.handler.JvmGcMetricMessageHandler;
import com.sbss.bithon.server.metric.handler.JvmMetricMessageHandler;
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
public class KafkaMetricCollector extends AbstractKafkaCollector<GenericMetricMessage> {

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
                                SqlMetricMessageHandler sqlMetricMessageHandler) {
        super(GenericMetricMessage.class);
        localSink = new LocalMetricSink(jvmMetricMessageHandler,
                                        jvmGcMetricMessageHandler,
                                        webRequestMetricMessageHandler,
                                        webServerMetricMessageHandler,
                                        exceptionMetricMessageHandler,
                                        httpClientMetricMessageHandler,
                                        threadPoolMetricMessageHandler,
                                        jdbcPoolMetricMessageHandler,
                                        redisMetricMessageHandler,
                                        sqlMetricMessageHandler);
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
    protected void onMessage(String topic, GenericMetricMessage metric) {
        localSink.process(topic, metric);
    }
}
