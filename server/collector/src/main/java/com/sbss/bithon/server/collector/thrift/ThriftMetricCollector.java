package com.sbss.bithon.server.collector.thrift;

import com.sbss.bithon.agent.rpc.thrift.service.MessageHeader;
import com.sbss.bithon.agent.rpc.thrift.service.metric.IMetricCollector;
import com.sbss.bithon.agent.rpc.thrift.service.metric.message.ExceptionMetricMessage;
import com.sbss.bithon.agent.rpc.thrift.service.metric.message.GcEntity;
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
import com.sbss.bithon.server.common.utils.collection.SizedIterator;
import com.sbss.bithon.server.metric.handler.GenericMetricMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/10 2:37 下午
 */
@Slf4j
public class ThriftMetricCollector implements IMetricCollector.Iface {

    private final IMessageSink<SizedIterator<GenericMetricMessage>> metricSink;

    public ThriftMetricCollector(IMessageSink<SizedIterator<GenericMetricMessage>> metricSink) {
        this.metricSink = metricSink;
    }

    @Override
    public void sendWebRequest(MessageHeader header, List<WebRequestMetricMessage> messages) {
        if (CollectionUtils.isEmpty(messages)) {
            return;
        }

        metricSink.process("web-request-metrics", new GenericMetricMessageSizedIterator(header, messages));
    }

    @Override
    public void sendJvm(MessageHeader header, List<JvmMetricMessage> messages) {
        if (CollectionUtils.isEmpty(messages)) {
            return;
        }

        final int size = messages.size();
        final Iterator<JvmMetricMessage> iterator = messages.iterator();
        metricSink.process("jvm-metrics", new SizedIterator<GenericMetricMessage>(){
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public GenericMetricMessage next() {
                return GenericMetricMessage.of(header, iterator.next());
            }

            @Override
            public void close() {
            }

            @Override
            public int size() {
                return size;
            }
        });

        final List<GcEntity> gcList = messages.stream()
                                              .flatMap(jvm -> jvm.gcEntities.stream())
                                              .collect(Collectors.toList());
        final Iterator<GcEntity> gc = gcList.iterator();
        metricSink.process("jvm-gc-metrics", new SizedIterator<GenericMetricMessage>() {
            @Override
            public int size() {
                return gcList.size();
            }

            @Override
            public void close() {
            }

            @Override
            public boolean hasNext() {
                return gc.hasNext();
            }

            @Override
            public GenericMetricMessage next() {
                GenericMetricMessage message = GenericMetricMessage.of(header, gc.next());
                message.set("timestamp", messages.get(0).timestamp);
                return message;
            }
        });
    }

    @Override
    public void sendWebServer(MessageHeader header, List<WebServerMetricMessage> messages) {
        if (CollectionUtils.isEmpty(messages)) {
            return;
        }

        metricSink.process("web-server-metrics", new GenericMetricMessageSizedIterator(header, messages));
    }

    @Override
    public void sendException(MessageHeader header, List<ExceptionMetricMessage> messages) {
        if (CollectionUtils.isEmpty(messages)) {
            return;
        }

        metricSink.process("exception-metrics", new GenericMetricMessageSizedIterator(header, messages));
    }

    @Override
    public void sendHttpClient(MessageHeader header, List<HttpClientMetricMessage> messages) {
        if (CollectionUtils.isEmpty(messages)) {
            return;
        }

        metricSink.process("exception-metrics", new GenericMetricMessageSizedIterator(header, messages));
    }

    @Override
    public void sendThreadPool(MessageHeader header, List<ThreadPoolMetricMessage> messages) {
        if (CollectionUtils.isEmpty(messages)) {
            return;
        }

        metricSink.process("thread-pool-metrics", new GenericMetricMessageSizedIterator(header, messages));
    }

    @Override
    public void sendJdbc(MessageHeader header, List<JdbcPoolMetricMessage> messages) {
        if (CollectionUtils.isEmpty(messages)) {
            return;
        }

        metricSink.process("jdbc-pool-metrics", new GenericMetricMessageSizedIterator(header, messages));
    }

    @Override
    public void sendRedis(MessageHeader header, List<RedisMetricMessage> messages) {
        if (CollectionUtils.isEmpty(messages)) {
            return;
        }

        metricSink.process("redis-metrics", new GenericMetricMessageSizedIterator(header, messages));
    }

    @Override
    public void sendSql(MessageHeader header, List<SqlMetricMessage> messages) {
        if (CollectionUtils.isEmpty(messages)) {
            return;
        }

        metricSink.process("sql-metrics", new GenericMetricMessageSizedIterator(header, messages));
    }

    @Override
    public void sendMongoDb(MessageHeader header, List<MongoDbMetricMessage> messages) {
        if (CollectionUtils.isEmpty(messages)) {
            return;
        }

        metricSink.process("mongo-metrics", new GenericMetricMessageSizedIterator(header, messages));
    }

    private static class GenericMetricMessageSizedIterator implements SizedIterator<GenericMetricMessage> {
        private final int size;
        private final Iterator<?> iterator;
        private final MessageHeader header;

        public GenericMetricMessageSizedIterator(MessageHeader header, List<?> messages) {
            this.header = header;
            this.size = messages.size();
            this.iterator = messages.iterator();
        }

        @Override
        public int size() {
            return size;
        }

        @Override
        public void close() {
        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public GenericMetricMessage next() {
            return GenericMetricMessage.of(header, iterator.next());
        }
    }
}
