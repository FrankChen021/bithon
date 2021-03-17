package com.sbss.bithon.server.collector.protocol;

import com.sbss.bithon.agent.rpc.thrift.service.MessageHeader;
import com.sbss.bithon.agent.rpc.thrift.service.metric.IMetricCollector;
import com.sbss.bithon.agent.rpc.thrift.service.metric.message.*;
import com.sbss.bithon.server.metric.handler.GenericMetricMessage;
import com.sbss.bithon.server.collector.sink.IMessageSink;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/10 2:37 下午
 */
@Slf4j
@Service
public class ThriftMetricCollector implements IMetricCollector.Iface {

    private final IMessageSink<GenericMetricMessage> metricSink;

    public ThriftMetricCollector(@Qualifier("metricSink") IMessageSink metricSink) {
        this.metricSink = metricSink;
    }

    @Override
    public void sendWebRequest(MessageHeader header, List<WebRequestMetricMessage> messages) {
        messages.forEach((message) -> {
            metricSink.process("web-request-metrics", GenericMetricMessage.of(header, message));
        });
    }

    @Override
    public void sendJvm(MessageHeader header, List<JvmMetricMessage> messages) {
        messages.forEach((message) -> {
            metricSink.process("jvm-metrics", GenericMetricMessage.of(header, message));
            metricSink.process("jvm-gc-metrics", GenericMetricMessage.of(header, message));
        });
    }

    @Override
    public void sendWebServer(MessageHeader header, List<WebServerMetricMessage> messages) {
        messages.forEach((message) -> {
            metricSink.process("web-server-metrics", GenericMetricMessage.of(header, message));
        });
    }

    @Override
    public void sendException(MessageHeader header, List<ExceptionMetricMessage> messages) {
        messages.forEach((message) -> {
            metricSink.process("exception-metrics", GenericMetricMessage.of(header, message));
        });
    }

    @Override
    public void sendHttpClient(MessageHeader header, List<HttpClientMetricMessage> messages) {
        messages.forEach((message) -> {
            metricSink.process("http-client-metrics", GenericMetricMessage.of(header, message));
        });
    }

    @Override
    public void sendThreadPool(MessageHeader header, List<ThreadPoolMetricMessage> messages) {
        messages.forEach((message) -> {
            metricSink.process("thread-pool-metrics", GenericMetricMessage.of(header, message));
        });
    }

    @Override
    public void sendJdbc(MessageHeader header, List<JdbcPoolMetricMessage> messages) {
        messages.forEach((message) -> {
            metricSink.process("jdbc-pool-metrics", GenericMetricMessage.of(header, message));
        });
    }

    @Override
    public void sendRedis(MessageHeader header, List<RedisMetricMessage> messages) {
        messages.forEach((message) -> {
            metricSink.process("redis-metrics", GenericMetricMessage.of(header, message));
        });
    }
}
