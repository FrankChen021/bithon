package com.sbss.bithon.agent.dispatcher.thrift;

import com.sbss.bithon.agent.core.dispatcher.IMessageConverter;
import com.sbss.bithon.agent.core.event.EventMessage;
import com.sbss.bithon.agent.core.metric.domain.exception.ExceptionMetricSet;
import com.sbss.bithon.agent.core.metric.domain.http.HttpClientCompositeMetric;
import com.sbss.bithon.agent.core.metric.domain.jdbc.JdbcPoolMetricSet;
import com.sbss.bithon.agent.core.metric.domain.jvm.JvmMetricSet;
import com.sbss.bithon.agent.core.metric.domain.mongo.MongoDbCompositeMetric;
import com.sbss.bithon.agent.core.metric.domain.redis.RedisClientCompositeMetric;
import com.sbss.bithon.agent.core.metric.domain.sql.SqlCompositeMetric;
import com.sbss.bithon.agent.core.metric.domain.sql.SqlStatementCompositeMetric;
import com.sbss.bithon.agent.core.metric.domain.thread.ThreadPoolCompositeMetric;
import com.sbss.bithon.agent.core.metric.domain.web.WebRequestCompositeMetric;
import com.sbss.bithon.agent.core.metric.domain.web.WebServerMetricSet;
import com.sbss.bithon.agent.core.tracing.context.TraceSpan;
import com.sbss.bithon.agent.rpc.thrift.service.event.ThriftEventMessage;
import com.sbss.bithon.agent.rpc.thrift.service.metric.message.ClassEntity;
import com.sbss.bithon.agent.rpc.thrift.service.metric.message.CpuEntity;
import com.sbss.bithon.agent.rpc.thrift.service.metric.message.ExceptionMetricMessage;
import com.sbss.bithon.agent.rpc.thrift.service.metric.message.GcEntity;
import com.sbss.bithon.agent.rpc.thrift.service.metric.message.HeapEntity;
import com.sbss.bithon.agent.rpc.thrift.service.metric.message.HttpClientMetricMessage;
import com.sbss.bithon.agent.rpc.thrift.service.metric.message.InstanceTimeEntity;
import com.sbss.bithon.agent.rpc.thrift.service.metric.message.JdbcPoolMetricMessage;
import com.sbss.bithon.agent.rpc.thrift.service.metric.message.JvmMetricMessage;
import com.sbss.bithon.agent.rpc.thrift.service.metric.message.MemoryEntity;
import com.sbss.bithon.agent.rpc.thrift.service.metric.message.MetaspaceEntity;
import com.sbss.bithon.agent.rpc.thrift.service.metric.message.MongoDbMetricMessage;
import com.sbss.bithon.agent.rpc.thrift.service.metric.message.NonHeapEntity;
import com.sbss.bithon.agent.rpc.thrift.service.metric.message.RedisMetricMessage;
import com.sbss.bithon.agent.rpc.thrift.service.metric.message.SqlMetricMessage;
import com.sbss.bithon.agent.rpc.thrift.service.metric.message.ThreadEntity;
import com.sbss.bithon.agent.rpc.thrift.service.metric.message.ThreadPoolMetricMessage;
import com.sbss.bithon.agent.rpc.thrift.service.metric.message.WebRequestMetricMessage;
import com.sbss.bithon.agent.rpc.thrift.service.metric.message.WebServerMetricMessage;
import com.sbss.bithon.agent.rpc.thrift.service.trace.TraceSpanMessage;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/6 11:39 下午
 */
public class ToThriftMessageConverter implements IMessageConverter {

    @Override
    public Object from(long timestamp,
                       int interval,
                       List<String> dimensions,
                       HttpClientCompositeMetric metric) {
        HttpClientMetricMessage message = new HttpClientMetricMessage();
        message.setInterval(interval);
        message.setTimestamp(timestamp);
        message.setUri(dimensions.get(0));
        message.setMethod(dimensions.get(1));
        message.setResponseTime(metric.getResponseTime().getSum().get());
        message.setMinResponseTime(metric.getResponseTime().getMin().get());
        message.setMaxResponseTime(metric.getResponseTime().getMax().get());
        message.setCount4xx(metric.getCount4xx());
        message.setCount5xx(metric.getCount5xx());
        message.setRequestCount(metric.getRequestCount());
        message.setRequestBytes(metric.getRequestBytes());
        message.setResponseBytes(metric.getResponseBytes());
        message.setExceptionCount(metric.getExceptionCount());
        return message;
    }

    @Override
    public Object from(long timestamp, int interval, JdbcPoolMetricSet metric) {
        JdbcPoolMetricMessage message = new JdbcPoolMetricMessage();
        message.setTimestamp(timestamp);
        message.setInterval(interval);
        message.setConnectionString(metric.getConnectionString());
        message.setDriverClass(metric.getDriverClass());
        message.setActiveCount(metric.activeCount.get());
        message.setCreateCount(metric.createCount.get());
        message.setDestroyCount(metric.destroyCount.get());
        message.setPoolingPeak(metric.poolingPeak.get());
        message.setActivePeak(metric.activePeak.get());
        message.setLogicConnectCount(metric.logicConnectionCount.get());
        message.setLogicCloseCount(metric.logicCloseCount.get());
        message.setCreateErrorCount(metric.createErrorCount.get());
        message.setExecuteCount(metric.executeCount.get());
        message.setCommitCount(metric.commitCount.get());
        message.setRollbackCount(metric.rollbackCount.get());
        message.setStartTransactionCount(metric.startTransactionCount.get());
        return message;
    }

    @Override
    public Object from(long timestamp, int interval, List<String> dimensions, SqlCompositeMetric metric) {
        SqlMetricMessage message = new SqlMetricMessage();
        message.setTimestamp(timestamp);
        message.setConnectionString(dimensions.get(0));
        message.setInterval(interval);
        message.setCallCount(metric.getCallCount().get());
        message.setResponseTime(metric.getResponseTime().getSum().get());
        message.setMinResponseTime(metric.getResponseTime().getMin().get());
        message.setMaxResponseTime(metric.getResponseTime().getMax().get());
        message.setErrorCount(metric.getErrorCount().get());
        message.setQueryCount(metric.getQueryCount().get());
        message.setUpdateCount(metric.getUpdateCount().get());
        return message;
    }

    @Override
    public Object from(long timestamp,
                       int interval,
                       List<String> dimensions,
                       MongoDbCompositeMetric metric) {
        MongoDbMetricMessage message = new MongoDbMetricMessage();
        message.setInterval(interval);
        message.setTimestamp(timestamp);
        message.setServer(dimensions.get(0));
        message.setDatabase(dimensions.get(1));
        message.setCollection(null);
        message.setCommand(null);
        message.setResponseTime(metric.getResponseTime().getSum().get());
        message.setMaxResponseTime(metric.getResponseTime().getMax().get());
        message.setMinResponseTime(metric.getResponseTime().getMin().get());
        message.setCallCount(metric.getCallCount().get());
        message.setExceptionCount(metric.getExceptionCount().get());
        message.setRequestBytes(metric.getRequestBytes().get());
        message.setResponseBytes(metric.getResponseBytes().get());
        return message;
    }

    @Override
    public Object from(long timestamp,
                       int interval,
                       List<String> dimensions,
                       WebRequestCompositeMetric metric) {
        WebRequestMetricMessage message = new WebRequestMetricMessage();
        message.setInterval(interval);
        message.setTimestamp(timestamp);
        message.setSrcApplication(dimensions.get(0));
        message.setUri(dimensions.get(1));
        message.setResponseTime(metric.getResponseTime().getSum().get());
        message.setMaxResponseTime(metric.getResponseTime().getMax().get());
        message.setMinResponseTime(metric.getResponseTime().getMin().get());
        message.setCallCount(metric.getRequestCount().get());
        message.setErrorCount(metric.getErrorCount().get());
        message.setCount4xx(metric.getCount4xx().get());
        message.setCount5xx(metric.getCount5xx().get());
        message.setRequestBytes(metric.getRequestBytes().get());
        message.setResponseBytes(metric.getResponseBytes().get());
        return message;
    }

    @Override
    public Object from(long timestamp,
                       int interval,
                       JvmMetricSet metric) {
        JvmMetricMessage message = new JvmMetricMessage();
        message.setInterval(interval);
        message.setTimestamp(timestamp);

        message.setInstanceTimeEntity(new InstanceTimeEntity(metric.upTime, metric.startTime));
        message.setCpuEntity(new CpuEntity(metric.cpuMetricsSet.processorNumber,
                                           metric.cpuMetricsSet.processCpuTime,
                                           metric.cpuMetricsSet.avgSystemLoad,
                                           metric.cpuMetricsSet.processCpuLoad));
        message.setMemoryEntity(new MemoryEntity(metric.memoryMetricsSet.allocatedBytes,
                                                 metric.memoryMetricsSet.freeBytes));
        message.setHeapEntity(new HeapEntity(metric.heapMetricsSet.heapBytes,
                                             metric.heapMetricsSet.heapInitBytes,
                                             metric.heapMetricsSet.heapUsedBytes,
                                             metric.heapMetricsSet.heapAvailableBytes));
        message.setNonHeapEntity(new NonHeapEntity(metric.nonHeapMetricsSet.nonHeapBytes,
                                                   metric.nonHeapMetricsSet.nonHeapInitBytes,
                                                   metric.nonHeapMetricsSet.nonHeapUsedBytes,
                                                   metric.nonHeapMetricsSet.nonHeapAvailableBytes));
        message.setThreadEntity(new ThreadEntity(metric.threadMetricsSet.peakActiveCount,
                                                 metric.threadMetricsSet.activeDaemonCount,
                                                 metric.threadMetricsSet.totalCreatedCount,
                                                 metric.threadMetricsSet.activeThreadsCount));
        message.setGcEntities(metric.gcCompositeMetrics.stream().map(gcMetric -> {
            GcEntity e = new GcEntity(gcMetric.generation,
                                      gcMetric.gcCount,
                                      gcMetric.gcTime);
            e.setGcName(gcMetric.gcName);
            return e;
        }).collect(Collectors.toList()));

        message.setClassesEntity(new ClassEntity(metric.classMetricsSet.currentLoadedClasses,
                                                 metric.classMetricsSet.totalLoadedClasses,
                                                 metric.classMetricsSet.totalUnloadedClasses));
        message.setMetaspaceEntity(new MetaspaceEntity(metric.metaspaceMetricsSet.metaspaceCommittedBytes,
                                                       metric.metaspaceMetricsSet.metaspaceUsedBytes));
        return message;
    }

    @Override
    public Object from(long timestamp,
                       int interval,
                       WebServerMetricSet metric) {
        WebServerMetricMessage message = new WebServerMetricMessage();
        message.setInterval(interval);
        message.setTimestamp(timestamp);
        message.setConnectionCount(metric.getConnectionCount());
        message.setMaxConnections(metric.getMaxConnections());
        message.setActiveThreads(metric.getActiveThreads());
        message.setMaxThreads(metric.getMaxThreads());
        message.setType(metric.getServerType().type());
        return message;
    }

    @Override
    public Object from(long timestamp, int interval, SqlStatementCompositeMetric counter) {
        return null;
    }

    @Override
    public Object from(long timestamp,
                       int interval,
                       List<String> dimensions,
                       RedisClientCompositeMetric metric) {
        RedisMetricMessage message = new RedisMetricMessage();
        message.setInterval(interval);
        message.setTimestamp(timestamp);
        message.setUri(dimensions.get(0));
        message.setCommand(dimensions.get(1));
        message.setExceptionCount(metric.getExceptionCount());
        message.setTotalCount(metric.getCallCount());
        message.setRequestTime(metric.getRequestTime().getSum().get());
        message.setResponseTime(metric.getResponseTime().getSum().get());
        message.setRequestBytes(metric.getRequestBytes());
        message.setResponseBytes(metric.getResponseBytes());
        return message;
    }

    @Override
    public Object from(long timestamp,
                       int interval,
                       ExceptionMetricSet metric) {
        ExceptionMetricMessage message = new ExceptionMetricMessage();
        message.setInterval(interval);
        message.setTimestamp(timestamp);
        message.setUri(metric.getUri());
        message.setMessage(metric.getMessage());
        message.setClassName(metric.getExceptionClass());
        message.setStackTrace(metric.getStackTrace());
        message.setExceptionCount(metric.getCount());
        return message;
    }

    @Override
    public Object from(TraceSpan span) {
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
    public Object from(long timestamp,
                       int interval,
                       ThreadPoolCompositeMetric metric) {
        ThreadPoolMetricMessage message = new ThreadPoolMetricMessage();
        message.setInterval(interval);
        message.setTimestamp(timestamp);
        message.setExecutorClass(metric.getExecutorClass());
        message.setPoolName(metric.getThreadPoolName());

        message.setActiveThreads(metric.getActiveThreads());
        message.setCurrentPoolSize(metric.getCurrentPoolSize());
        message.setMaxPoolSize(metric.getMaxPoolSize());
        message.setLargestPoolSize(metric.getLargestPoolSize());
        message.setQueuedTaskCount(metric.getQueuedTaskCount());
        message.setCallerRunTaskCount(metric.getCallerRunTaskCount());
        message.setAbortedTaskCount(metric.getAbortedTaskCount());
        message.setDiscardedTaskCount(metric.getDiscardedTaskCount());
        message.setDiscardedOldestTaskCount(metric.getDiscardedOldestTaskCount());
        message.setExceptionTaskCount(metric.getExceptionTaskCount());
        message.setSuccessfulTaskCount(metric.getSuccessfulTaskCount());
        message.setTotalTaskCount(metric.getTotalTaskCount());

        return message;
    }
}

