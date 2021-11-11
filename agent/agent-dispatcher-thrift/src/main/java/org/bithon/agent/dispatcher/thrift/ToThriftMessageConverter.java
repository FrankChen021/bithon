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

package org.bithon.agent.dispatcher.thrift;

import org.bithon.agent.core.dispatcher.IMessageConverter;
import org.bithon.agent.core.event.EventMessage;
import org.bithon.agent.core.metric.collector.IMeasurement;
import org.bithon.agent.core.metric.domain.exception.ExceptionMetrics;
import org.bithon.agent.core.metric.domain.jdbc.JdbcPoolMetrics;
import org.bithon.agent.core.metric.domain.jvm.GcMetrics;
import org.bithon.agent.core.metric.domain.jvm.JvmMetrics;
import org.bithon.agent.core.metric.domain.mongo.MongoDbMetrics;
import org.bithon.agent.core.metric.domain.redis.RedisClientMetrics;
import org.bithon.agent.core.metric.domain.sql.SQLMetrics;
import org.bithon.agent.core.metric.domain.sql.SQLStatementMetrics;
import org.bithon.agent.core.metric.domain.thread.ThreadPoolMetrics;
import org.bithon.agent.core.metric.domain.web.WebServerMetrics;
import org.bithon.agent.core.metric.model.schema.Schema;
import org.bithon.agent.core.metric.model.schema.Schema2;
import org.bithon.agent.core.tracing.context.ITraceSpan;
import org.bithon.agent.rpc.thrift.service.event.ThriftEventMessage;
import org.bithon.agent.rpc.thrift.service.metric.message.ExceptionMetricMessage;
import org.bithon.agent.rpc.thrift.service.metric.message.JdbcPoolMetricMessage;
import org.bithon.agent.rpc.thrift.service.metric.message.JvmGcMetricMessage;
import org.bithon.agent.rpc.thrift.service.metric.message.JvmMetricMessage;
import org.bithon.agent.rpc.thrift.service.metric.message.MongoDbMetricMessage;
import org.bithon.agent.rpc.thrift.service.metric.message.RedisMetricMessage;
import org.bithon.agent.rpc.thrift.service.metric.message.SqlMetricMessage;
import org.bithon.agent.rpc.thrift.service.metric.message.ThreadPoolMetricMessage;
import org.bithon.agent.rpc.thrift.service.metric.message.WebServerMetricMessage;
import org.bithon.agent.rpc.thrift.service.trace.TraceSpanMessage;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/6 11:39 下午
 */
public class ToThriftMessageConverter implements IMessageConverter {

    @Override
    public Object from(long timestamp, int interval, JdbcPoolMetrics metrics) {
        JdbcPoolMetricMessage message = new JdbcPoolMetricMessage();
        message.setTimestamp(timestamp);
        message.setInterval(interval);
        message.setConnectionString(metrics.getConnectionString());
        message.setDriverClass(metrics.getDriverClass());
        message.setActiveCount(metrics.activeCount.get());
        message.setCreateCount(metrics.createCount.get());
        message.setDestroyCount(metrics.destroyCount.get());
        message.setPoolingCount(metrics.poolingCount.get());
        message.setPoolingPeak(metrics.poolingPeak.get());
        message.setActivePeak(metrics.activePeak.get());
        message.setLogicConnectCount(metrics.logicConnectionCount.get());
        message.setLogicCloseCount(metrics.logicCloseCount.get());
        message.setCreateErrorCount(metrics.createErrorCount.get());
        message.setExecuteCount(metrics.executeCount.get());
        message.setCommitCount(metrics.commitCount.get());
        message.setRollbackCount(metrics.rollbackCount.get());
        message.setStartTransactionCount(metrics.startTransactionCount.get());
        message.setWaitThreadCount(metrics.waitThreadCount.get());
        return message;
    }

    @Override
    public Object from(long timestamp, int interval, List<String> dimensions, SQLMetrics metrics) {
        SqlMetricMessage message = new SqlMetricMessage();
        message.setTimestamp(timestamp);
        message.setConnectionString(dimensions.get(0));
        message.setInterval(interval);
        message.setCallCount(metrics.getCallCount().get());
        message.setResponseTime(metrics.getResponseTime().getSum().get());
        message.setMinResponseTime(metrics.getResponseTime().getMin().get());
        message.setMaxResponseTime(metrics.getResponseTime().getMax().get());
        message.setErrorCount(metrics.getErrorCount().get());
        message.setQueryCount(metrics.getQueryCount().get());
        message.setUpdateCount(metrics.getUpdateCount().get());
        return message;
    }

    @Override
    public Object from(long timestamp,
                       int interval,
                       List<String> dimensions,
                       MongoDbMetrics metrics) {
        MongoDbMetricMessage message = new MongoDbMetricMessage();
        message.setInterval(interval);
        message.setTimestamp(timestamp);
        message.setServer(dimensions.get(0));
        message.setDatabase(dimensions.get(1));
        message.setCollection(null);
        message.setCommand(null);
        message.setResponseTime(metrics.getResponseTime().getSum().get());
        message.setMaxResponseTime(metrics.getResponseTime().getMax().get());
        message.setMinResponseTime(metrics.getResponseTime().getMin().get());
        message.setCallCount(metrics.getCallCount().get());
        message.setExceptionCount(metrics.getExceptionCount().get());
        message.setRequestBytes(metrics.getRequestBytes().get());
        message.setResponseBytes(metrics.getResponseBytes().get());
        return message;
    }

    @Override
    public Object from(long timestamp, int interval, JvmMetrics metrics) {
        JvmMetricMessage message = new JvmMetricMessage();
        message.setInterval(interval);
        message.setTimestamp(timestamp);

        message.instanceStartTime = metrics.startTime;
        message.instanceUpTime = metrics.upTime;

        message.processors = metrics.cpu.processorNumber;
        message.processCpuLoad = metrics.cpu.processCpuLoad;
        message.processCpuTime = metrics.cpu.processCpuTime;
        message.systemLoadAvg = metrics.cpu.avgSystemLoad;

        message.totalMemBytes = metrics.memory.allocatedBytes;
        message.freeMemBytes = metrics.memory.freeBytes;

        message.heapMax = metrics.heap.max;
        message.heapInit = metrics.heap.init;
        message.heapCommitted = metrics.heap.committed;
        message.heapUsed = metrics.heap.used;

        message.nonHeapMax = metrics.nonHeap.max;
        message.nonHeapInit = metrics.nonHeap.init;
        message.nonHeapCommitted = metrics.nonHeap.committed;
        message.nonHeapUsed = metrics.nonHeap.used;

        message.peakThreads = metrics.thread.peakActiveCount;
        message.activeThreads = metrics.thread.activeThreadsCount;
        message.daemonThreads = metrics.thread.activeDaemonCount;
        message.totalThreads = metrics.thread.totalCreatedCount;

        message.classLoaded = metrics.clazz.currentLoadedClasses;
        message.classUnloaded = metrics.clazz.totalUnloadedClasses;

        message.metaspaceCommitted = metrics.metaspace.committed;
        message.metaspaceUsed = metrics.metaspace.used;
        message.metaspaceInit = metrics.metaspace.init;
        message.metaspaceMax = metrics.metaspace.max;

        message.directMax = metrics.directMemory.max;
        message.directUsed = metrics.directMemory.used;
        return message;
    }

    @Override
    public Object from(long timestamp,
                       int interval,
                       WebServerMetrics metrics) {
        WebServerMetricMessage message = new WebServerMetricMessage();
        message.setInterval(interval);
        message.setTimestamp(timestamp);
        message.setConnectionCount(metrics.getConnectionCount());
        message.setMaxConnections(metrics.getMaxConnections());
        message.setActiveThreads(metrics.getActiveThreads());
        message.setMaxThreads(metrics.getMaxThreads());
        message.setType(metrics.getServerType().type());
        return message;
    }

    @Override
    public Object from(long timestamp, int interval, SQLStatementMetrics metrics) {
        return null;
    }

    @Override
    public Object from(long timestamp,
                       int interval,
                       List<String> dimensions,
                       RedisClientMetrics metrics) {
        RedisMetricMessage message = new RedisMetricMessage();
        message.setInterval(interval);
        message.setTimestamp(timestamp);
        message.setUri(dimensions.get(0));
        message.setCommand(dimensions.get(1));
        message.setExceptionCount(metrics.getExceptionCount());
        message.setTotalCount(metrics.getCallCount());

        message.setMinRequestTime(metrics.getRequestTime().getMin().get());
        message.setRequestTime(metrics.getRequestTime().getSum().get());
        message.setMaxRequestTime(metrics.getRequestTime().getMax().get());

        message.setMinResponseTime(metrics.getResponseTime().getMin().get());
        message.setResponseTime(metrics.getResponseTime().getSum().get());
        message.setMaxResponseTime(metrics.getResponseTime().getMax().get());

        message.setRequestBytes(metrics.getRequestBytes());
        message.setResponseBytes(metrics.getResponseBytes());
        return message;
    }

    @Override
    public Object from(long timestamp,
                       int interval,
                       ExceptionMetrics metrics) {
        ExceptionMetricMessage message = new ExceptionMetricMessage();
        message.setInterval(interval);
        message.setTimestamp(timestamp);
        message.setUri(metrics.getUri());
        message.setMessage(metrics.getMessage());
        message.setClassName(metrics.getExceptionClass());
        message.setStackTrace(metrics.getStackTrace());
        message.setExceptionCount(metrics.getCount());
        return message;
    }

    @Override
    public Object from(ITraceSpan span) {
        TraceSpanMessage spanMessage = new TraceSpanMessage();
        spanMessage.setTraceId(span.traceId());
        spanMessage.setSpanId(span.spanId());
        spanMessage.setParentSpanId(span.parentSpanId());
        spanMessage.setStartTime(span.startTime());
        spanMessage.setEndTime(span.endTime());
        spanMessage.setKind(span.kind().toString());
        spanMessage.setName(span.component());
        spanMessage.setClazz(span.clazz());
        spanMessage.setMethod(span.method());
        spanMessage.setTags(span.tags());
        spanMessage.setParentAppName(span.parentApplication());
        return spanMessage;
    }

    @Override
    public Object from(EventMessage event) {
        ThriftEventMessage message = new ThriftEventMessage();
        message.setTimestamp(System.currentTimeMillis());
        message.setEventType(event.getMessageType());
        message.setArguments(event.getArgs());
        return message;
    }

    @Override
    public Object from(Map<String, String> log) {
        return null;
    }

    @Override
    public Object from(long timestamp, int interval, GcMetrics metrics) {
        JvmGcMetricMessage message = new JvmGcMetricMessage();
        message.setInterval(interval);
        message.setTimestamp(timestamp);
        message.setInterval(interval);
        message.setTimestamp(timestamp);
        message.setGcName(metrics.getGcName());
        message.setGeneration(metrics.getGeneration());
        message.setGcCount(metrics.getGcCount());
        message.setGcTime(metrics.getGcTime());
        return message;
    }

    @Override
    public Object from(Schema schema, Collection<IMeasurement> measurementList, long timestamp, int interval) {
        return null;
    }

    @Override
    public Object from(Schema2 schema, Collection<IMeasurement> measurementList, long timestamp, int interval) {
        return null;
    }

    @Override
    public Object from(long timestamp,
                       int interval,
                       ThreadPoolMetrics metrics) {
        ThreadPoolMetricMessage message = new ThreadPoolMetricMessage();
        message.setInterval(interval);
        message.setTimestamp(timestamp);
        message.setExecutorClass(metrics.getExecutorClass());
        message.setPoolName(metrics.getThreadPoolName());

        message.setActiveThreads(metrics.getActiveThreads());
        message.setCurrentPoolSize(metrics.getCurrentPoolSize());
        message.setMaxPoolSize(metrics.getMaxPoolSize());
        message.setLargestPoolSize(metrics.getLargestPoolSize());
        message.setQueuedTaskCount(metrics.getQueuedTaskCount());
        message.setCallerRunTaskCount(metrics.getCallerRunTaskCount());
        message.setAbortedTaskCount(metrics.getAbortedTaskCount());
        message.setDiscardedTaskCount(metrics.getDiscardedTaskCount());
        message.setDiscardedOldestTaskCount(metrics.getDiscardedOldestTaskCount());
        message.setExceptionTaskCount(metrics.getExceptionTaskCount());
        message.setSuccessfulTaskCount(metrics.getSuccessfulTaskCount());
        message.setTotalTaskCount(metrics.getTotalTaskCount());

        return message;
    }
}

