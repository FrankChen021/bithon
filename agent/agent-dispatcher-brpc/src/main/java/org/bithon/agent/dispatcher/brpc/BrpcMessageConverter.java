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

package org.bithon.agent.dispatcher.brpc;

import org.bithon.agent.core.dispatcher.IMessageConverter;
import org.bithon.agent.core.event.EventMessage;
import org.bithon.agent.core.metric.collector.IMetricSet;
import org.bithon.agent.core.metric.domain.exception.ExceptionMetricSet;
import org.bithon.agent.core.metric.domain.http.HttpOutgoingMetrics;
import org.bithon.agent.core.metric.domain.jdbc.JdbcPoolMetricSet;
import org.bithon.agent.core.metric.domain.jvm.GcCompositeMetric;
import org.bithon.agent.core.metric.domain.jvm.JvmMetricSet;
import org.bithon.agent.core.metric.domain.mongo.MongoDbCompositeMetric;
import org.bithon.agent.core.metric.domain.redis.RedisClientCompositeMetric;
import org.bithon.agent.core.metric.domain.sql.SqlCompositeMetric;
import org.bithon.agent.core.metric.domain.sql.SqlStatementCompositeMetric;
import org.bithon.agent.core.metric.domain.thread.ThreadPoolCompositeMetric;
import org.bithon.agent.core.metric.domain.web.WebServerMetricSet;
import org.bithon.agent.core.metric.model.schema.Schema;
import org.bithon.agent.core.metric.model.schema.Schema2;
import org.bithon.agent.core.tracing.context.ITraceSpan;
import org.bithon.agent.rpc.brpc.event.BrpcEventMessage;
import org.bithon.agent.rpc.brpc.metrics.BrpcExceptionMetricMessage;
import org.bithon.agent.rpc.brpc.metrics.BrpcGenericDimensionSpec;
import org.bithon.agent.rpc.brpc.metrics.BrpcGenericMetricMessage;
import org.bithon.agent.rpc.brpc.metrics.BrpcGenericMetricMessageV2;
import org.bithon.agent.rpc.brpc.metrics.BrpcGenericMetricSchema;
import org.bithon.agent.rpc.brpc.metrics.BrpcGenericMetricSchemaV2;
import org.bithon.agent.rpc.brpc.metrics.BrpcGenericMetricSet;
import org.bithon.agent.rpc.brpc.metrics.BrpcGenericMetricSpec;
import org.bithon.agent.rpc.brpc.metrics.BrpcHttpOutgoingMetricMessage;
import org.bithon.agent.rpc.brpc.metrics.BrpcJdbcPoolMetricMessage;
import org.bithon.agent.rpc.brpc.metrics.BrpcJvmGcMetricMessage;
import org.bithon.agent.rpc.brpc.metrics.BrpcJvmMetricMessage;
import org.bithon.agent.rpc.brpc.metrics.BrpcRedisMetricMessage;
import org.bithon.agent.rpc.brpc.metrics.BrpcThreadPoolMetricMessage;
import org.bithon.agent.rpc.brpc.metrics.BrpcWebServerMetricMessage;
import org.bithon.agent.rpc.brpc.tracing.BrpcTraceSpanMessage;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/6/27 20:13
 */
public class BrpcMessageConverter implements IMessageConverter {
    @Override
    public Object from(long timestamp, int interval, List<String> dimensions, HttpOutgoingMetrics metric) {
        return BrpcHttpOutgoingMetricMessage.newBuilder()
                                            .setTimestamp(timestamp)
                                            .setInterval(interval)
                                            .setUri(dimensions.get(0))
                                            .setMethod(dimensions.get(1))
                                            .setMaxResponseTime(metric.getResponseTime().getMax().get())
                                            .setMinResponseTime(metric.getResponseTime().getMin().get())
                                            .setResponseTime(metric.getResponseTime().getSum().get())
                                            .setCount4Xx(metric.getCount4xx())
                                            .setCount5Xx(metric.getCount5xx())
                                            .setExceptionCount(metric.getExceptionCount())
                                            .setRequestCount(metric.getRequestCount())
                                            .setRequestBytes(metric.getRequestBytes())
                                            .setResponseBytes(metric.getResponseBytes())
                                            .build();
    }

    @Override
    public Object from(long timestamp, int interval, JdbcPoolMetricSet metric) {
        return BrpcJdbcPoolMetricMessage.newBuilder()
                                        .setTimestamp(timestamp)
                                        .setInterval(interval)
                                        .setConnectionString(metric.getConnectionString())
                                        .setDriverClass(metric.getDriverClass())
                                        .setActiveCount(metric.activeCount.get())
                                        .setActivePeak(metric.activePeak.get())
                                        .setPoolingCount(metric.poolingCount.get())
                                        .setPoolingPeak(metric.poolingPeak.get())
                                        .setCreateCount(metric.createCount.get())
                                        .setDestroyCount(metric.destroyCount.get())
                                        .setLogicCloseCount(metric.logicCloseCount.get())
                                        .setLogicCloseCount(metric.logicCloseCount.get())
                                        .setCreateErrorCount(metric.createErrorCount.get())
                                        .setExecuteCount(metric.executeCount.get())
                                        .setCommitCount(metric.commitCount.get())
                                        .setRollbackCount(metric.rollbackCount.get())
                                        .setStartTransactionCount(metric.startTransactionCount.get())
                                        .setWaitThreadCount(metric.waitThreadCount.get())
                                        .build();
    }

    @Override
    public Object from(long timestamp, int interval, List<String> dimensions, SqlCompositeMetric metric) {
        return null;
    }

    @Override
    public Object from(long timestamp, int interval, List<String> dimensions, MongoDbCompositeMetric counter) {
        return null;
    }

    @Override
    public Object from(long timestamp, int interval, JvmMetricSet metric) {
        BrpcJvmMetricMessage.Builder builder = BrpcJvmMetricMessage.newBuilder();
        builder.setInterval(interval);
        builder.setTimestamp(timestamp);
        builder.setInstanceStartTime(metric.startTime);
        builder.setInstanceUpTime(metric.upTime);
        builder.setProcessors(metric.cpuMetricSet.processorNumber);
        builder.setProcessCpuLoad(metric.cpuMetricSet.processCpuLoad);
        builder.setProcessCpuTime(metric.cpuMetricSet.processCpuTime);
        builder.setSystemLoadAvg(metric.cpuMetricSet.avgSystemLoad);
        builder.setTotalMemBytes(metric.memoryMetricSet.allocatedBytes);
        builder.setFreeMemBytes(metric.memoryMetricSet.freeBytes);
        builder.setHeapMax(metric.heapMetricSet.max);
        builder.setHeapInit(metric.heapMetricSet.init);
        builder.setHeapCommitted(metric.heapMetricSet.committed);
        builder.setHeapUsed(metric.heapMetricSet.used);
        builder.setNonHeapMax(metric.nonHeapMetricSet.max);
        builder.setNonHeapInit(metric.nonHeapMetricSet.init);
        builder.setNonHeapCommitted(metric.nonHeapMetricSet.committed);
        builder.setNonHeapUsed(metric.nonHeapMetricSet.used);
        builder.setPeakThreads(metric.threadMetricSet.peakActiveCount);
        builder.setActiveThreads(metric.threadMetricSet.activeThreadsCount);
        builder.setDaemonThreads(metric.threadMetricSet.activeDaemonCount);
        builder.setTotalThreads(metric.threadMetricSet.totalCreatedCount);
        builder.setClassLoaded(metric.classMetricSet.currentLoadedClasses);
        builder.setClassUnloaded(metric.classMetricSet.totalUnloadedClasses);
        builder.setMetaspaceCommitted(metric.metaspaceMetricSet.committed);
        builder.setMetaspaceUsed(metric.metaspaceMetricSet.used);
        builder.setMetaspaceInit(metric.metaspaceMetricSet.init);
        builder.setMetaspaceMax(metric.metaspaceMetricSet.max);
        builder.setDirectMax(metric.directMemMetricSet.max);
        builder.setDirectUsed(metric.directMemMetricSet.used);
        return builder.build();
    }

    @Override
    public Object from(long timestamp, int interval, WebServerMetricSet metric) {
        return BrpcWebServerMetricMessage.newBuilder()
                                         .setTimestamp(timestamp)
                                         .setInterval(interval)
                                         .setActiveThreads(metric.getActiveThreads())
                                         .setConnectionCount(metric.getConnectionCount())
                                         .setMaxConnections(metric.getMaxConnections())
                                         .setMaxThreads(metric.getMaxThreads())
                                         .setType(metric.getServerType().toString())
                                         .build();
    }

    @Override
    public Object from(long timestamp, int interval, SqlStatementCompositeMetric counter) {
        return null;
    }

    @Override
    public Object from(long timestamp, int interval, List<String> dimensions, RedisClientCompositeMetric metric) {
        return BrpcRedisMetricMessage.newBuilder()
                                     .setTimestamp(timestamp)
                                     .setInterval(interval)
                                     .setUri(dimensions.get(0))
                                     .setCommand(dimensions.get(1))
                                     .setMinRequestTime(metric.getRequestTime().getMin().get())
                                     .setRequestTime(metric.getRequestTime().getSum().get())
                                     .setMaxRequestTime(metric.getRequestTime().getMax().get())
                                     .setMinResponseTime(metric.getResponseTime().getMin().get())
                                     .setResponseTime(metric.getResponseTime().getSum().get())
                                     .setMaxResponseTime(metric.getResponseTime().getMax().get())
                                     .setTotalCount(metric.getCallCount())
                                     .setExceptionCount(metric.getExceptionCount())
                                     .setRequestBytes(metric.getRequestBytes())
                                     .setResponseBytes(metric.getResponseBytes())
                                     .build();
    }

    @Override
    public Object from(long timestamp, int interval, ExceptionMetricSet metric) {
        BrpcExceptionMetricMessage.Builder builder = BrpcExceptionMetricMessage.newBuilder()
                                                                               .setTimestamp(timestamp)
                                                                               .setInterval(interval);
        if (metric.getUri() != null) {
            builder.setUri(metric.getUri());
        }
        if (metric.getMessage() != null) {
            builder.setMessage(metric.getMessage());
        }
        return builder.setExceptionCount(metric.getCount())
                      .setClassName(metric.getExceptionClass())
                      .build();
    }

    @Override
    public Object from(long timestamp, int interval, ThreadPoolCompositeMetric metric) {
        return BrpcThreadPoolMetricMessage.newBuilder().setTimestamp(timestamp)
                                          .setInterval(interval)
                                          .setExecutorClass(metric.getExecutorClass())
                                          .setPoolName(metric.getThreadPoolName())
                                          // task
                                          .setCallerRunTaskCount(metric.getCallerRunTaskCount())
                                          .setAbortedTaskCount(metric.getAbortedTaskCount())
                                          .setDiscardedOldestTaskCount(metric.getDiscardedOldestTaskCount())
                                          .setDiscardedTaskCount(metric.getDiscardedTaskCount())
                                          .setExceptionTaskCount(metric.getExceptionTaskCount())
                                          .setSuccessfulTaskCount(metric.getSuccessfulTaskCount())
                                          .setTotalTaskCount(metric.getTotalTaskCount())
                                          // thread pool
                                          .setActiveThreads(metric.getActiveThreads())
                                          .setCurrentPoolSize(metric.getCurrentPoolSize())
                                          .setMaxPoolSize(metric.getMaxPoolSize())
                                          .setLargestPoolSize(metric.getLargestPoolSize())
                                          .build();
    }

    @Override
    public Object from(ITraceSpan span) {
        BrpcTraceSpanMessage.Builder builder = BrpcTraceSpanMessage.newBuilder()
                                                                   .setTraceId(span.traceId())
                                                                   .setSpanId(span.spanId())
                                                                   .setStartTime(span.startTime())
                                                                   .setEndTime(span.endTime())
                                                                   .setKind(span.kind().toString())
                                                                   .setName(span.component())
                                                                   .setClazz(span.clazz())
                                                                   .setMethod(span.method())
                                                                   .putAllTags(span.tags());
        if (span.parentApplication() != null) {
            builder.setParentAppName(span.parentApplication());
        }
        if (span.parentSpanId() != null) {
            builder.setParentSpanId(span.parentSpanId());
        }
        return builder.build();
    }

    @Override
    public Object from(EventMessage event) {
        return BrpcEventMessage.newBuilder()
                               .setTimestamp(System.currentTimeMillis())
                               .setEventType(event.getMessageType())
                               .putAllArguments(event.getArgs() == null ? Collections.emptyMap() : event.getArgs())
                               .build();
    }

    @Override
    public Object from(Map<String, String> log) {
        return null;
    }

    @Override
    public Object from(long timestamp, int interval, GcCompositeMetric gcMetricSet) {
        return BrpcJvmGcMetricMessage.newBuilder().setTimestamp(timestamp)
                                     .setInterval(interval)
                                     .setGcName(gcMetricSet.getGcName())
                                     .setGeneration(gcMetricSet.getGeneration())
                                     .setGcTime(gcMetricSet.getGcTime())
                                     .setGcCount(gcMetricSet.getGcCount())
                                     .build();
    }

    @Override
    public Object from(Schema schema,
                       Collection<IMetricSet> metricCollection,
                       long timestamp,
                       int interval) {
        BrpcGenericMetricSchema.Builder schemaBuilder = BrpcGenericMetricSchema.newBuilder()
                                                                               .setName(schema.getName());
        schema.getDimensionsSpec().forEach(dimensionSpec -> schemaBuilder.addDimensionsSpec(BrpcGenericDimensionSpec.newBuilder()
                                                                                                                    .setName(dimensionSpec.getName())
                                                                                                                    .setType(dimensionSpec.getType())
                                                                                                                    .build()));
        schema.getMetricsSpec().forEach(metricSpec -> schemaBuilder.addMetricsSpec(BrpcGenericMetricSpec.newBuilder()
                                                                                                        .setName(metricSpec.getName())
                                                                                                        .setType(metricSpec.getType())
                                                                                                        .build()));

        BrpcGenericMetricMessage.Builder messageBuilder = BrpcGenericMetricMessage.newBuilder();
        messageBuilder.setSchema(schemaBuilder.build());
        messageBuilder.setInterval(interval);
        messageBuilder.setTimestamp(timestamp);

        metricCollection.forEach(metricSet -> {
            BrpcGenericMetricSet.Builder set = BrpcGenericMetricSet.newBuilder();
            // although dimensions are defined as List<String>
            // but it could also store Object
            // we use Object.toString here to get right value
            for (Object dimension : metricSet.getDimensions()) {
                set.addDimension(dimension.toString());
            }
            for (int i = 0, size = metricSet.getMetricCount(); i < size; i++) {
                set.addMetric(metricSet.getMetricValue(i));
            }
            messageBuilder.addMetricSet(set.build());
        });

        return messageBuilder.build();
    }

    @Override
    public Object from(Schema2 schema, Collection<IMetricSet> metricCollection, long timestamp, int interval) {
        BrpcGenericMetricSchemaV2.Builder schemaBuilder = BrpcGenericMetricSchemaV2.newBuilder()
                                                                                   .setName(schema.getName());
        schema.getDimensionsSpec().forEach(schemaBuilder::addDimensionsSpec);
        schema.getMetricsSpec().forEach(schemaBuilder::addMetricsSpec);

        BrpcGenericMetricMessageV2.Builder messageBuilder = BrpcGenericMetricMessageV2.newBuilder();
        messageBuilder.setSchema(schemaBuilder.build());
        messageBuilder.setInterval(interval);
        messageBuilder.setTimestamp(timestamp);

        metricCollection.forEach(metricSet -> {
            BrpcGenericMetricSet.Builder set = BrpcGenericMetricSet.newBuilder();
            // although dimensions are defined as List<String>
            // but it could also store Object
            // we use Object.toString here to get right value
            for (Object dimension : metricSet.getDimensions()) {
                set.addDimension(dimension.toString());
            }
            for (int i = 0, size = metricSet.getMetricCount(); i < size; i++) {
                set.addMetric(metricSet.getMetricValue(i));
            }
            messageBuilder.addMetricSet(set.build());
        });

        return messageBuilder.build();
    }
}
