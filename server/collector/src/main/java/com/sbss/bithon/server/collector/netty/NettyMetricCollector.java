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

package com.sbss.bithon.server.collector.netty;

import cn.bithon.rpc.services.IMetricCollector;
import cn.bithon.rpc.services.MessageHeader;
import cn.bithon.rpc.services.metrics.ExceptionMetricMessage;
import cn.bithon.rpc.services.metrics.HttpClientMetricMessage;
import cn.bithon.rpc.services.metrics.JdbcPoolMetricMessage;
import cn.bithon.rpc.services.metrics.JvmGcMetricMessage;
import cn.bithon.rpc.services.metrics.JvmMetricMessage;
import cn.bithon.rpc.services.metrics.MongoDbMetricMessage;
import cn.bithon.rpc.services.metrics.RedisMetricMessage;
import cn.bithon.rpc.services.metrics.SqlMetricMessage;
import cn.bithon.rpc.services.metrics.ThreadPoolMetricMessage;
import cn.bithon.rpc.services.metrics.WebRequestMetricMessage;
import cn.bithon.rpc.services.metrics.WebServerMetricMessage;
import com.sbss.bithon.server.collector.sink.IMessageSink;
import com.sbss.bithon.server.common.utils.collection.CloseableIterator;
import com.sbss.bithon.server.metric.handler.GenericMetricMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.Iterator;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/10 2:37 下午
 */
@Slf4j
public class NettyMetricCollector implements IMetricCollector {

    private final IMessageSink<CloseableIterator<GenericMetricMessage>> metricSink;

    public NettyMetricCollector(IMessageSink<CloseableIterator<GenericMetricMessage>> metricSink) {
        this.metricSink = metricSink;
    }

    @Override
    public void sendWebRequest(MessageHeader header, List<WebRequestMetricMessage> messages) {
        if (CollectionUtils.isEmpty(messages)) {
            return;
        }

        metricSink.process("web-request-metrics", new GenericMetricMessageIterator(header, messages));
    }

    @Override
    public void sendJvm(MessageHeader header, List<JvmMetricMessage> messages) {
        if (CollectionUtils.isEmpty(messages)) {
            return;
        }

        metricSink.process("jvm-metrics", new GenericMetricMessageIterator(header, messages));
    }

    @Override
    public void sendJvmGc(MessageHeader header, List<JvmGcMetricMessage> messages) {
        if (CollectionUtils.isEmpty(messages)) {
            return;
        }

        metricSink.process("jvm-gc-metrics", new GenericMetricMessageIterator(header, messages));
    }

    @Override
    public void sendWebServer(MessageHeader header, List<WebServerMetricMessage> messages) {
        if (CollectionUtils.isEmpty(messages)) {
            return;
        }

        metricSink.process("web-server-metrics", new GenericMetricMessageIterator(header, messages));
    }

    @Override
    public void sendException(MessageHeader header, List<ExceptionMetricMessage> messages) {
        if (CollectionUtils.isEmpty(messages)) {
            return;
        }

        metricSink.process("exception-metrics", new GenericMetricMessageIterator(header, messages));
    }

    @Override
    public void sendHttpClient(MessageHeader header, List<HttpClientMetricMessage> messages) {
        if (CollectionUtils.isEmpty(messages)) {
            return;
        }

        metricSink.process("http-client-metrics", new GenericMetricMessageIterator(header, messages));
    }

    @Override
    public void sendThreadPool(MessageHeader header, List<ThreadPoolMetricMessage> messages) {
        if (CollectionUtils.isEmpty(messages)) {
            return;
        }

        metricSink.process("thread-pool-metrics", new GenericMetricMessageIterator(header, messages));
    }

    @Override
    public void sendJdbc(MessageHeader header, List<JdbcPoolMetricMessage> messages) {
        if (CollectionUtils.isEmpty(messages)) {
            return;
        }

        metricSink.process("jdbc-pool-metrics", new GenericMetricMessageIterator(header, messages));
    }

    @Override
    public void sendRedis(MessageHeader header, List<RedisMetricMessage> messages) {
        if (CollectionUtils.isEmpty(messages)) {
            return;
        }

        metricSink.process("redis-metrics", new GenericMetricMessageIterator(header, messages));
    }

    @Override
    public void sendSql(MessageHeader header, List<SqlMetricMessage> messages) {
        if (CollectionUtils.isEmpty(messages)) {
            return;
        }

        metricSink.process("sql-metrics", new GenericMetricMessageIterator(header, messages));
    }

    @Override
    public void sendMongoDb(MessageHeader header, List<MongoDbMetricMessage> messages) {
        if (CollectionUtils.isEmpty(messages)) {
            return;
        }

        metricSink.process("mongodb-metrics", new GenericMetricMessageIterator(header, messages));
    }

    private static class GenericMetricMessageIterator implements CloseableIterator<GenericMetricMessage> {
        private final Iterator<?> iterator;
        private final MessageHeader header;

        public GenericMetricMessageIterator(MessageHeader header, List<?> messages) {
            this.header = header;
            this.iterator = messages.iterator();
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
