package com.sbss.bithon.server.collector.thrift;

import com.sbss.bithon.agent.rpc.thrift.service.MessageHeader;
import com.sbss.bithon.agent.rpc.thrift.service.metric.IMetricCollector;
import com.sbss.bithon.agent.rpc.thrift.service.metric.message.ExceptionMetricMessage;
import com.sbss.bithon.agent.rpc.thrift.service.metric.message.HttpClientMetricMessage;
import com.sbss.bithon.agent.rpc.thrift.service.metric.message.JdbcPoolMetricMessage;
import com.sbss.bithon.agent.rpc.thrift.service.metric.message.JvmMetricMessage;
import com.sbss.bithon.agent.rpc.thrift.service.metric.message.MongoDbMetricMessage;
import com.sbss.bithon.agent.rpc.thrift.service.metric.message.RedisMetricMessage;
import com.sbss.bithon.agent.rpc.thrift.service.metric.message.SqlMetricMessage;
import com.sbss.bithon.agent.rpc.thrift.service.metric.message.ThreadPoolMetricMessage;
import com.sbss.bithon.agent.rpc.thrift.service.metric.message.WebRequestMetricMessage;
import com.sbss.bithon.agent.rpc.thrift.service.metric.message.WebServerMetricMessage;
import com.sbss.bithon.server.collector.sink.IMessageSink;
import com.sbss.bithon.server.metric.handler.GenericMetricMessage;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/10 2:37 下午
 */
@Slf4j
public class ThriftMetricCollector implements IMetricCollector.Iface {

    private final IMessageSink<GenericMetricMessage> metricSink;

    public ThriftMetricCollector(IMessageSink<GenericMetricMessage> metricSink) {
        this.metricSink = metricSink;
    }

    @Override
    public void sendWebRequest(MessageHeader header, List<WebRequestMetricMessage> messages) {
        messages.forEach((message) -> metricSink.process("web-request-metrics",
                                                         GenericMetricMessage.of(header, message)));
    }

    @Override
    public void sendJvm(MessageHeader header, List<JvmMetricMessage> messages) {
        messages.forEach((message) -> {
            metricSink.process("jvm-metrics", GenericMetricMessage.of(header, message));

            message.gcEntities.forEach((gc) -> {
                GenericMetricMessage gcMessage = GenericMetricMessage.of(header, gc);
                gcMessage.set("interval", message.interval);
                gcMessage.set("timestamp", message.timestamp);
                metricSink.process("jvm-gc-metrics", gcMessage);
            });
        });
    }

    @Override
    public void sendWebServer(MessageHeader header, List<WebServerMetricMessage> messages) {
        messages.forEach((message) -> metricSink.process("web-server-metrics",
                                                         GenericMetricMessage.of(header, message)));
    }

    @Override
    public void sendException(MessageHeader header, List<ExceptionMetricMessage> messages) {
        messages.forEach((message) -> {
            if (message.getExceptionCount() > 0) {
                metricSink.process("exception-metrics",
                                   GenericMetricMessage.of(header, message));
            }
        });
    }

    @Override
    public void sendHttpClient(MessageHeader header, List<HttpClientMetricMessage> messages) {
        messages.forEach((message) -> {
            //MAY NOT Correct since this message contains other metrics
            //if (message.getRequestCount() > 0) {
            metricSink.process("http-client-metrics",
                               GenericMetricMessage.of(header, message));
            //}
        });
    }

    @Override
    public void sendThreadPool(MessageHeader header, List<ThreadPoolMetricMessage> messages) {
        messages.forEach((message) -> metricSink.process("thread-pool-metrics",
                                                         GenericMetricMessage.of(header, message)));
    }

    @Override
    public void sendJdbc(MessageHeader header, List<JdbcPoolMetricMessage> messages) {
        messages.forEach((message) -> metricSink.process("jdbc-pool-metrics",
                                                         GenericMetricMessage.of(header, message)));
    }

    @Override
    public void sendRedis(MessageHeader header, List<RedisMetricMessage> messages) {
        messages.forEach((message) -> metricSink.process("redis-metrics", GenericMetricMessage.of(header, message)));
    }

    @Override
    public void sendSql(MessageHeader header, List<SqlMetricMessage> messages) {
        messages.forEach((message) -> metricSink.process("sql-metrics", GenericMetricMessage.of(header, message)));
    }

    @Override
    public void sendMongoDb(MessageHeader header, List<MongoDbMetricMessage> messages) {
        messages.forEach((message) -> metricSink.process("mongodb-metrics", GenericMetricMessage.of(header, message)));
    }
}
