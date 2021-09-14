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

package com.sbss.bithon.server.collector.brpc;


import com.sbss.bithon.agent.rpc.brpc.BrpcMessageHeader;
import com.sbss.bithon.agent.rpc.brpc.metrics.BrpcExceptionMetricMessage;
import com.sbss.bithon.agent.rpc.brpc.metrics.BrpcHttpIncomingMetricMessage;
import com.sbss.bithon.agent.rpc.brpc.metrics.BrpcHttpOutgoingMetricMessage;
import com.sbss.bithon.agent.rpc.brpc.metrics.BrpcJdbcPoolMetricMessage;
import com.sbss.bithon.agent.rpc.brpc.metrics.BrpcJvmGcMetricMessage;
import com.sbss.bithon.agent.rpc.brpc.metrics.BrpcJvmMetricMessage;
import com.sbss.bithon.agent.rpc.brpc.metrics.BrpcMongoDbMetricMessage;
import com.sbss.bithon.agent.rpc.brpc.metrics.BrpcRedisMetricMessage;
import com.sbss.bithon.agent.rpc.brpc.metrics.BrpcSqlMetricMessage;
import com.sbss.bithon.agent.rpc.brpc.metrics.BrpcThreadPoolMetricMessage;
import com.sbss.bithon.agent.rpc.brpc.metrics.BrpcWebServerMetricMessage;
import com.sbss.bithon.agent.rpc.brpc.metrics.IMetricCollector;
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
public class BrpcMetricCollector implements IMetricCollector {

    private final IMessageSink<CloseableIterator<GenericMetricMessage>> metricSink;

    public BrpcMetricCollector(IMessageSink<CloseableIterator<GenericMetricMessage>> metricSink) {
        this.metricSink = metricSink;
    }

    @Override
    public void sendIncomingHttp(BrpcMessageHeader header, List<BrpcHttpIncomingMetricMessage> messages) {
        if (CollectionUtils.isEmpty(messages)) {
            return;
        }

        metricSink.process("http-incoming-metrics", new GenericMetricMessageIterator(header, messages));
    }

    @Override
    public void sendJvm(BrpcMessageHeader header, List<BrpcJvmMetricMessage> messages) {
        if (CollectionUtils.isEmpty(messages)) {
            return;
        }

        metricSink.process("jvm-metrics", new GenericMetricMessageIterator(header, messages));
    }

    @Override
    public void sendJvmGc(BrpcMessageHeader header, List<BrpcJvmGcMetricMessage> messages) {
        if (CollectionUtils.isEmpty(messages)) {
            return;
        }

        metricSink.process("jvm-gc-metrics", new GenericMetricMessageIterator(header, messages));
    }

    @Override
    public void sendWebServer(BrpcMessageHeader header, List<BrpcWebServerMetricMessage> messages) {
        if (CollectionUtils.isEmpty(messages)) {
            return;
        }

        metricSink.process("web-server-metrics", new GenericMetricMessageIterator(header, messages));
    }

    @Override
    public void sendException(BrpcMessageHeader header, List<BrpcExceptionMetricMessage> messages) {
        if (CollectionUtils.isEmpty(messages)) {
            return;
        }

        metricSink.process("exception-metrics", new GenericMetricMessageIterator(header, messages));
    }

    @Override
    public void sendOutgoingHttp(BrpcMessageHeader header, List<BrpcHttpOutgoingMetricMessage> messages) {
        if (CollectionUtils.isEmpty(messages)) {
            return;
        }

        metricSink.process("http-outgoing-metrics", new GenericMetricMessageIterator(header, messages));
    }

    @Override
    public void sendThreadPool(BrpcMessageHeader header, List<BrpcThreadPoolMetricMessage> messages) {
        if (CollectionUtils.isEmpty(messages)) {
            return;
        }

        metricSink.process("thread-pool-metrics", new GenericMetricMessageIterator(header, messages));
    }

    @Override
    public void sendJdbc(BrpcMessageHeader header, List<BrpcJdbcPoolMetricMessage> messages) {
        if (CollectionUtils.isEmpty(messages)) {
            return;
        }

        metricSink.process("jdbc-pool-metrics", new GenericMetricMessageIterator(header, messages));
    }

    @Override
    public void sendRedis(BrpcMessageHeader header, List<BrpcRedisMetricMessage> messages) {
        if (CollectionUtils.isEmpty(messages)) {
            return;
        }

        metricSink.process("redis-metrics", new GenericMetricMessageIterator(header, messages));
    }

    @Override
    public void sendSql(BrpcMessageHeader header, List<BrpcSqlMetricMessage> messages) {
        if (CollectionUtils.isEmpty(messages)) {
            return;
        }

        metricSink.process("sql-metrics", new GenericMetricMessageIterator(header, messages));
    }

    @Override
    public void sendMongoDb(BrpcMessageHeader header, List<BrpcMongoDbMetricMessage> messages) {
        if (CollectionUtils.isEmpty(messages)) {
            return;
        }

        metricSink.process("mongodb-metrics", new GenericMetricMessageIterator(header, messages));
    }

    private static class GenericMetricMessageIterator implements CloseableIterator<GenericMetricMessage> {
        private final Iterator<?> iterator;
        private final BrpcMessageHeader header;

        public GenericMetricMessageIterator(BrpcMessageHeader header, List<?> messages) {
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
