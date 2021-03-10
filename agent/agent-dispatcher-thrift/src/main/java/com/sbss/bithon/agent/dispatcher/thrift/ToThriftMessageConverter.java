package com.sbss.bithon.agent.dispatcher.thrift;

import com.sbss.bithon.agent.core.context.AppInstance;
import com.sbss.bithon.agent.core.dispatcher.IMessageConverter;
import com.sbss.bithon.agent.core.events.EventMessage;
import com.sbss.bithon.agent.core.metrics.exception.ExceptionMetric;
import com.sbss.bithon.agent.core.metrics.http.HttpClientMetric;
import com.sbss.bithon.agent.core.metrics.jdbc.JdbcPoolMetric;
import com.sbss.bithon.agent.core.metrics.jvm.JvmMetrics;
import com.sbss.bithon.agent.core.metrics.mongo.MongoMetric;
import com.sbss.bithon.agent.core.metrics.redis.RedisMetric;
import com.sbss.bithon.agent.core.metrics.sql.SqlMetric;
import com.sbss.bithon.agent.core.metrics.sql.SqlStatementMetric;
import com.sbss.bithon.agent.core.metrics.thread.ThreadPoolMetrics;
import com.sbss.bithon.agent.core.metrics.web.WebRequestMetric;
import com.sbss.bithon.agent.core.metrics.web.WebServerMetric;
import com.sbss.bithon.agent.core.tracing.context.TraceSpan;
import com.sbss.bithon.agent.rpc.thrift.service.event.ThriftEventMessage;
import com.sbss.bithon.agent.rpc.thrift.service.metric.message.*;
import com.sbss.bithon.agent.rpc.thrift.service.trace.TraceSpanMessage;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/6 11:39 下午
 */
public class ToThriftMessageConverter implements IMessageConverter {

    @Override
    public Object from(AppInstance appInstance, long timestamp, int interval, HttpClientMetric metric) {
        HttpClientMetricMessage message = new HttpClientMetricMessage();
        message.setInterval(interval);
        message.setTimestamp(timestamp);
        message.setUri(metric.getUri());
        message.setMethod(metric.getMethod());
        message.setCostTime(metric.getCostTime());
        message.setCount4xx(metric.getCount4xx());
        message.setCount5xx(metric.getCount5xx());
        message.setRequestCount(metric.getRequestCount());
        message.setRequestBytes(metric.getRequestBytes());
        message.setResponseBytes(metric.getResponseBytes());
        message.setExceptionCount(metric.getExceptionCount());
        return message;
    }

    @Override
    public Object from(AppInstance appInstance, long timestamp, int interval, JdbcPoolMetric metric) {
        JdbcPoolMetricMessage message = new JdbcPoolMetricMessage();

        message.setInterval(interval);
        message.setTimestamp(timestamp);
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
    public Object from(AppInstance appInstance, long timestamp, int interval, SqlMetric metric) {
        return null;
    }

    @Override
    public Object from(AppInstance appInstance, long timestamp, int interval, MongoMetric counter) {
        return null;
    }

    @Override
    public Object from(AppInstance appInstance,
                       long timestamp,
                       int interval,
                       WebRequestMetric metric) {
        WebRequestMetricMessage message = new WebRequestMetricMessage();
        message.setInterval(interval);
        message.setTimestamp(timestamp);
        message.setSrcApplication(metric.getSrcApplication());
        message.setUri(metric.getUri());
        message.setCostTime(metric.getCostTime());
        message.setRequestCount(metric.getRequestCount());
        message.setErrorCount(metric.getErrorCount());
        message.setCount4xx(metric.getCount4xx());
        message.setCount5xx(metric.getCount5xx());
        message.setRequestByteSize(metric.getRequestByteSize());
        message.setResponseByteSize(metric.getResponseByteSize());
        return message;
    }

    @Override
    public Object from(AppInstance appInstance,
                       long timestamp,
                       int interval,
                       JvmMetrics metric) {
        JvmMetricMessage message = new JvmMetricMessage();
        message.setInterval(interval);
        message.setTimestamp(timestamp);

        message.setInstanceTimeEntity(new InstanceTimeEntity(metric.upTime, metric.startTime));
        message.setCpuEntity(new CpuEntity(metric.cpuMetrics.processorNumber,
                                                 metric.cpuMetrics.processCpuTime,
                                                 metric.cpuMetrics.avgSystemLoad,
                                                 metric.cpuMetrics.processCpuLoad));
        message.setMemoryEntity(new MemoryEntity(metric.memoryMetrics.allocatedBytes,
                                                       metric.memoryMetrics.freeBytes));
        message.setHeapEntity(new HeapEntity(metric.heapMetrics.heapBytes,
                                                   metric.heapMetrics.heapInitBytes,
                                                   metric.heapMetrics.heapUsedBytes,
                                                   metric.heapMetrics.heapCommittedBytes));
        message.setNonHeapEntity(new NonHeapEntity(metric.nonHeapMetrics.nonHeapBytes,
                                                         metric.nonHeapMetrics.nonHeapInitBytes,
                                                         metric.nonHeapMetrics.nonHeapUsedBytes,
                                                         metric.nonHeapMetrics.nonHeapCommitted));
        message.setThreadEntity(new ThreadEntity(metric.threadMetrics.peakActiveCount,
                                                       metric.threadMetrics.activeDaemonCount,
                                                       metric.threadMetrics.totalCreatedCount,
                                                       metric.threadMetrics.activeThreadsCount));
        message.setGcEntities(metric.gcMetrics.stream().map(gcEntity -> {
            GcEntity e = new GcEntity(gcEntity.gcCount,
                                      gcEntity.gcTime);
            e.setGcName(gcEntity.gcName);
            return e;
        }).collect(Collectors.toList()));

        message.setClassesEntity(new ClassEntity(metric.classMetrics.currentClassCount,
                                                       metric.classMetrics.loadedClassCount,
                                                       metric.classMetrics.unloadedClassCount));
        message.setMetaspaceEntity(new MetaspaceEntity(metric.metaspaceMetrics.metaspaceCommittedBytes,
                                                             metric.metaspaceMetrics.metaspaceUsedBytes));
        return message;
    }

    @Override
    public Object from(AppInstance appInstance,
                       long timestamp,
                       int interval,
                       WebServerMetric metric) {
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
    public Object from(SqlStatementMetric counter) {
        return null;
    }

    @Override
    public Object from(AppInstance appInstance, Map<String, String> map) {
        return null;
    }

    @Override
    public Object from(AppInstance appInstance, long timestamp, int interval, RedisMetric metric) {
        RedisMetricMessage message = new RedisMetricMessage();
        message.setInterval(interval);
        message.setTimestamp(timestamp);
        message.setUri(metric.getHostAndPort());
        message.setCommand(metric.getCommand());
        message.setExceptionCount(metric.getExceptionCount());
        message.setTotalCount(metric.getTotalCount());
        message.setRequestTime(metric.getRequestTime());
        message.setResponseTime(metric.getResponseTime());
        message.setRequestBytes(metric.getRequestBytes());
        message.setResponseBytes(metric.getResponseBytes());
        return message;
    }

    @Override
    public Object from(AppInstance appInstance,
                       long timestamp,
                       int interval,
                       ExceptionMetric metric) {
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
    public Object from(AppInstance appInstance, EventMessage event) {
        ThriftEventMessage message = new ThriftEventMessage();
        message.setTimestamp(System.currentTimeMillis());
        message.setEventType(event.getMessageType());
        message.setArguments(event.getArgs());
        return message;
    }

    @Override
    public Object from(AppInstance appInstance,
                       long timestamp,
                       int interval,
                       ThreadPoolMetrics metric) {
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

