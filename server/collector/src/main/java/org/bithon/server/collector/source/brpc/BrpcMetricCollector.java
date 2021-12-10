/*
 *    Copyright 2020 bithon.org
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

package org.bithon.server.collector.source.brpc;


import lombok.extern.slf4j.Slf4j;
import org.bithon.agent.rpc.brpc.BrpcMessageHeader;
import org.bithon.agent.rpc.brpc.metrics.BrpcExceptionMetricMessage;
import org.bithon.agent.rpc.brpc.metrics.BrpcGenericMeasurement;
import org.bithon.agent.rpc.brpc.metrics.BrpcGenericMetricMessage;
import org.bithon.agent.rpc.brpc.metrics.BrpcGenericMetricMessageV2;
import org.bithon.agent.rpc.brpc.metrics.BrpcHttpIncomingMetricMessage;
import org.bithon.agent.rpc.brpc.metrics.BrpcHttpOutgoingMetricMessage;
import org.bithon.agent.rpc.brpc.metrics.BrpcJdbcPoolMetricMessage;
import org.bithon.agent.rpc.brpc.metrics.BrpcJvmGcMetricMessage;
import org.bithon.agent.rpc.brpc.metrics.BrpcJvmMetricMessage;
import org.bithon.agent.rpc.brpc.metrics.BrpcMongoDbMetricMessage;
import org.bithon.agent.rpc.brpc.metrics.BrpcRedisMetricMessage;
import org.bithon.agent.rpc.brpc.metrics.BrpcSqlMetricMessage;
import org.bithon.agent.rpc.brpc.metrics.BrpcThreadPoolMetricMessage;
import org.bithon.agent.rpc.brpc.metrics.BrpcWebServerMetricMessage;
import org.bithon.agent.rpc.brpc.metrics.IMetricCollector;
import org.bithon.server.common.utils.ReflectionUtils;
import org.bithon.server.common.utils.collection.IteratorableCollection;
import org.bithon.server.metric.DataSourceSchema;
import org.bithon.server.metric.TimestampSpec;
import org.bithon.server.metric.aggregator.spec.IMetricSpec;
import org.bithon.server.metric.aggregator.spec.LongLastMetricSpec;
import org.bithon.server.metric.aggregator.spec.LongMaxMetricSpec;
import org.bithon.server.metric.aggregator.spec.LongMinMetricSpec;
import org.bithon.server.metric.aggregator.spec.LongSumMetricSpec;
import org.bithon.server.metric.dimension.IDimensionSpec;
import org.bithon.server.metric.dimension.StringDimensionSpec;
import org.bithon.server.metric.sink.IMessageSink;
import org.bithon.server.metric.sink.IMetricMessageSink;
import org.bithon.server.metric.sink.MetricMessage;
import org.bithon.server.metric.sink.SchemaMetricMessage;
import org.springframework.util.CollectionUtils;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/10 2:37 下午
 */
@Slf4j
public class BrpcMetricCollector implements IMetricCollector, AutoCloseable {

    private final IMessageSink<SchemaMetricMessage> schemaMetricSink;
    private final IMetricMessageSink metricSink;

    public BrpcMetricCollector(IMessageSink<SchemaMetricMessage> schemaMetricSink,
                               IMetricMessageSink metricSink) {
        this.schemaMetricSink = schemaMetricSink;
        this.metricSink = metricSink;
    }

    @Override
    public void sendIncomingHttp(BrpcMessageHeader header, List<BrpcHttpIncomingMetricMessage> messages) {
        if (CollectionUtils.isEmpty(messages)) {
            return;
        }

        metricSink.process("http-incoming-metrics", IteratorableCollection.of(new GenericMetricMessageIterator(header, messages)));
    }

    @Override
    public void sendJvm(BrpcMessageHeader header, List<BrpcJvmMetricMessage> messages) {
        if (CollectionUtils.isEmpty(messages)) {
            return;
        }

        metricSink.process("jvm-metrics", IteratorableCollection.of(new GenericMetricMessageIterator(header, messages)));
    }

    @Override
    public void sendJvmGc(BrpcMessageHeader header, List<BrpcJvmGcMetricMessage> messages) {
        if (CollectionUtils.isEmpty(messages)) {
            return;
        }

        metricSink.process("jvm-gc-metrics", IteratorableCollection.of(new GenericMetricMessageIterator(header, messages)));
    }

    @Override
    public void sendWebServer(BrpcMessageHeader header, List<BrpcWebServerMetricMessage> messages) {
        if (CollectionUtils.isEmpty(messages)) {
            return;
        }

        metricSink.process("web-server-metrics", IteratorableCollection.of(new GenericMetricMessageIterator(header, messages)));
    }

    @Override
    public void sendException(BrpcMessageHeader header, List<BrpcExceptionMetricMessage> messages) {
        if (CollectionUtils.isEmpty(messages)) {
            return;
        }

        metricSink.process("exception-metrics", IteratorableCollection.of(new GenericMetricMessageIterator(header, messages)));
    }

    @Override
    public void sendOutgoingHttp(BrpcMessageHeader header, List<BrpcHttpOutgoingMetricMessage> messages) {
        if (CollectionUtils.isEmpty(messages)) {
            return;
        }

        metricSink.process("http-outgoing-metrics", IteratorableCollection.of(new GenericMetricMessageIterator(header, messages)));
    }

    @Override
    public void sendThreadPool(BrpcMessageHeader header, List<BrpcThreadPoolMetricMessage> messages) {
        if (CollectionUtils.isEmpty(messages)) {
            return;
        }

        metricSink.process("thread-pool-metrics", IteratorableCollection.of(new GenericMetricMessageIterator(header, messages)));
    }

    @Override
    public void sendJdbc(BrpcMessageHeader header, List<BrpcJdbcPoolMetricMessage> messages) {
        if (CollectionUtils.isEmpty(messages)) {
            return;
        }

        metricSink.process("jdbc-pool-metrics", IteratorableCollection.of(new GenericMetricMessageIterator(header, messages)));
    }

    @Override
    public void sendRedis(BrpcMessageHeader header, List<BrpcRedisMetricMessage> messages) {
        if (CollectionUtils.isEmpty(messages)) {
            return;
        }

        metricSink.process("redis-metrics", IteratorableCollection.of(new GenericMetricMessageIterator(header, messages)));
    }

    @Override
    public void sendSql(BrpcMessageHeader header, List<BrpcSqlMetricMessage> messages) {
        if (CollectionUtils.isEmpty(messages)) {
            return;
        }

        metricSink.process("sql-metrics", IteratorableCollection.of(new GenericMetricMessageIterator(header, messages)));
    }

    @Override
    public void sendMongoDb(BrpcMessageHeader header, List<BrpcMongoDbMetricMessage> messages) {
        if (CollectionUtils.isEmpty(messages)) {
            return;
        }

        metricSink.process("mongodb-metrics", IteratorableCollection.of(new GenericMetricMessageIterator(header, messages)));
    }

    @Override
    public void sendGenericMetrics(BrpcMessageHeader header, BrpcGenericMetricMessage message) {
        SchemaMetricMessage schemaMetricMessage = new SchemaMetricMessage();

        DataSourceSchema schema = new DataSourceSchema(message.getSchema().getName(),
                                                       message.getSchema().getName(),
                                                       new TimestampSpec("timestamp", "auto", null),
                                                       message.getSchema().getDimensionsSpecList()
                                                              .stream()
                                                              .map(dimSpec -> new StringDimensionSpec(dimSpec.getName(),
                                                                                                      dimSpec.getName(),
                                                                                                      true,
                                                                                                      true,
                                                                                                      null))
                                                              .collect(Collectors.toList()),
                                                       message.getSchema().getMetricsSpecList()
                                                              .stream()
                                                              .map(metricSpec -> {
                                                                  if (metricSpec.getType().equals("longMax")) {
                                                                      return new LongMaxMetricSpec(metricSpec.getName(),
                                                                                                   metricSpec.getName(),
                                                                                                   "",
                                                                                                   true);
                                                                  }
                                                                  if (metricSpec.getType().equals("longMin")) {
                                                                      return new LongMinMetricSpec(metricSpec.getName(),
                                                                                                   metricSpec.getName(),
                                                                                                   "",
                                                                                                   true);
                                                                  }
                                                                  if (metricSpec.getType().equals("longSum")) {
                                                                      return new LongSumMetricSpec(metricSpec.getName(),
                                                                                                   metricSpec.getName(),
                                                                                                   "",
                                                                                                   true);
                                                                  }
                                                                  if (metricSpec.getType().equals("longLast")) {
                                                                      return new LongLastMetricSpec(metricSpec.getName(),
                                                                                                    metricSpec.getName(),
                                                                                                    "",
                                                                                                    true);
                                                                  }

                                                                  return null;
                                                              }).collect(Collectors.toList()),
                                                       null,
                                                       null);

        Iterator<BrpcGenericMeasurement> iterator = message.getMeasurementList().iterator();
        schemaMetricMessage.setSchema(schema);
        schemaMetricMessage.setMetrics(IteratorableCollection.of(new Iterator<MetricMessage>() {
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public MetricMessage next() {
                MetricMessage metricMessage = new MetricMessage();
                BrpcGenericMeasurement metricSet = iterator.next();

                int i = 0;
                for (String dimension : metricSet.getDimensionList()) {
                    IDimensionSpec dimensionSpec = schema.getDimensionsSpec().get(i++);
                    metricMessage.put(dimensionSpec.getName(), dimension);
                }

                i = 0;
                for (long metric : metricSet.getMetricList()) {
                    IMetricSpec metricSpec = schema.getMetricsSpec().get(i++);
                    metricMessage.put(metricSpec.getName(), metric);
                }

                metricMessage.put("interval", message.getInterval());
                metricMessage.put("timestamp", message.getTimestamp());
                ReflectionUtils.getFields(header, metricMessage);
                return metricMessage;
            }
        }));
        schemaMetricSink.process(message.getSchema().getName(), schemaMetricMessage);
    }

    @Override
    public void sendGenericMetricsV2(BrpcMessageHeader header, BrpcGenericMetricMessageV2 message) {
        Iterator<MetricMessage> mesageIterator = new Iterator<MetricMessage>() {
            final Iterator<BrpcGenericMeasurement> iterator = message.getMeasurementList().iterator();

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public MetricMessage next() {
                MetricMessage metricMessage = new MetricMessage();
                BrpcGenericMeasurement metricSet = iterator.next();

                int i = 0;
                for (String dimension : metricSet.getDimensionList()) {
                    String dimensionSpec = message.getSchema().getDimensionsSpec(i++);
                    metricMessage.put(dimensionSpec, dimension);
                }

                i = 0;
                for (long metric : metricSet.getMetricList()) {
                    String metricSpec = message.getSchema().getMetricsSpec(i++);
                    metricMessage.put(metricSpec, metric);
                }

                metricMessage.put("interval", message.getInterval());
                metricMessage.put("timestamp", message.getTimestamp());
                ReflectionUtils.getFields(header, metricMessage);
                return metricMessage;
            }
        };
        metricSink.process(message.getSchema().getName(), IteratorableCollection.of(mesageIterator));
    }

    @Override
    public void close() throws Exception {
        metricSink.close();
    }

    private static class GenericMetricMessageIterator implements Iterator<MetricMessage> {
        private final Iterator<?> iterator;
        private final BrpcMessageHeader header;

        public GenericMetricMessageIterator(BrpcMessageHeader header, List<?> messages) {
            this.header = header;
            this.iterator = messages.iterator();
        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public MetricMessage next() {
            return toMetricMessage(header, iterator.next());
        }

        private MetricMessage toMetricMessage(BrpcMessageHeader header, Object message) {
            MetricMessage metricMessage = new MetricMessage();
            ReflectionUtils.getFields(header, metricMessage);
            ReflectionUtils.getFields(message, metricMessage);

            // adaptor
            // protobuf turns the name 'count4xx' in .proto file to 'count4Xx'
            // we have to convert it back to make it compatible with existing name style
            Object count4xx = metricMessage.remove("count4Xx");
            if (count4xx != null) {
                metricMessage.put("count4xx", count4xx);
            }
            Object count5xx = metricMessage.remove("count5Xx");
            if (count5xx != null) {
                metricMessage.put("count5xx", count5xx);
            }

            return metricMessage;
        }
    }
}
