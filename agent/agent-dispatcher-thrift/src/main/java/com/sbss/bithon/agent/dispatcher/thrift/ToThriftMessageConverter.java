package com.sbss.bithon.agent.dispatcher.thrift;

import com.sbss.bithon.agent.core.context.AppInstance;
import com.sbss.bithon.agent.core.dispatcher.IMessageConverter;
import com.sbss.bithon.agent.core.events.EventMessage;
import com.sbss.bithon.agent.core.metrics.exception.ExceptionMetric;
import com.sbss.bithon.agent.core.metrics.http.HttpClientMetric;
import com.sbss.bithon.agent.core.metrics.jdbc.JdbcMetric;
import com.sbss.bithon.agent.core.metrics.jvm.JvmMetrics;
import com.sbss.bithon.agent.core.metrics.mongo.MongoMetric;
import com.sbss.bithon.agent.core.metrics.redis.RedisMetric;
import com.sbss.bithon.agent.core.metrics.sql.SqlMetric;
import com.sbss.bithon.agent.core.metrics.sql.SqlStatementMetric;
import com.sbss.bithon.agent.core.metrics.thread.AbstractThreadPoolMetrics;
import com.sbss.bithon.agent.core.metrics.web.WebRequestMetric;
import com.sbss.bithon.agent.core.metrics.web.WebServerMetric;
import com.sbss.bithon.agent.rpc.thrift.service.event.ThriftEventMessage;
import com.sbss.bithon.agent.rpc.thrift.service.metric.message.*;
import com.sbss.bithon.agent.rpc.thrift.service.trace.TraceMessage;
import com.sbss.bithon.agent.rpc.thrift.service.trace.TraceSpan;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/6 11:39 下午
 */
public class ToThriftMessageConverter implements IMessageConverter {
    @Override
    public Object from(AppInstance appInstance, long timestamp, int interval, HttpClientMetric metric) {
        HttpClientMessage thriftMessage = new HttpClientMessage();
        thriftMessage.setAppName(appInstance.getAppName());
        thriftMessage.setEnv(appInstance.getEnv());
        thriftMessage.setHostName(appInstance.getHostIp());
        thriftMessage.setPort(appInstance.getPort());
        thriftMessage.setInterval(interval);
        thriftMessage.setTimestamp(timestamp);
        HttpClientEntity entity = new HttpClientEntity();
        entity.setUri(metric.getUri());
        entity.setMethod(metric.getMethod());
        entity.setCostTime(metric.getCostTime());
        entity.setCount4xx(metric.getCount4xx());
        entity.setCount5xx(metric.getCount5xx());
        entity.setRequestCount(metric.getRequestCount());
        entity.setRequestBytes(metric.getRequestBytes());
        entity.setResponseBytes(metric.getResponseBytes());
        entity.setExceptionCount(metric.getExceptionCount());
        thriftMessage.setHttpClient(entity);
        return thriftMessage;
    }

    @Override
    public Object from(AppInstance appInstance, long timestamp, int interval, JdbcMetric metric) {
        JdbcMessage thriftMessage = new JdbcMessage();
        thriftMessage.setAppName(appInstance.getAppName());
        thriftMessage.setEnv(appInstance.getEnv());
        thriftMessage.setHostName(appInstance.getHostIp());
        thriftMessage.setPort(appInstance.getPort());
        thriftMessage.setInterval(interval);
        thriftMessage.setTimestamp(timestamp);

        JdbcEntity entity = new JdbcEntity();
        entity.setUri(metric.getUri());
        entity.setDriver(metric.getDriverType());
        entity.setActiveCount(metric.activeCount.get());
        entity.setCreateCount(metric.createCount.get());
        entity.setDestroyCount(metric.destroyCount.get());
        entity.setPoolingPeak(metric.poolingPeak.get());
        entity.setActivePeak(metric.activePeak.get());
        entity.setLogicConnectCount(metric.logicConnectionCount.get());
        entity.setLogicCloseCount(metric.logicCloseCount.get());
        entity.setCreateErrorCount(metric.createErrorCount.get());
        entity.setExecuteCount(metric.executeCount.get());
        entity.setCommitCount(metric.commitCount.get());
        entity.setRollbackCount(metric.rollbackCount.get());
        entity.setStartTransactionCount(metric.startTransactionCount.get());
        thriftMessage.setJdbcList(Collections.singletonList(entity));
        return thriftMessage;
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
        WebRequestMessage thriftMessage = new WebRequestMessage();
        thriftMessage.setAppName(appInstance.getAppName());
        thriftMessage.setEnv(appInstance.getEnv());
        thriftMessage.setHostName(appInstance.getHostIp());
        thriftMessage.setPort(appInstance.getPort());
        thriftMessage.setInterval(interval);
        thriftMessage.setTimestamp(timestamp);
        WebRequestEntity entity = new WebRequestEntity();
        entity.setUri(metric.getUri());
        entity.setCostTime(metric.getCostTime());
        entity.setRequestCount(metric.getRequestCount());
        entity.setErrorCount(metric.getErrorCount());
        entity.setCount4xx(metric.getCount4xx());
        entity.setCount5xx(metric.getCount5xx());
        entity.setRequestByteSize(metric.getRequestByteSize());
        entity.setResponseByteSize(metric.getResponseByteSize());
        thriftMessage.setRequestEntity(entity);
        return thriftMessage;
    }

    @Override
    public Object from(AppInstance appInstance,
                       long timestamp,
                       int interval,
                       JvmMetrics metric) {
        JvmMessage thriftMessage = new JvmMessage();
        thriftMessage.setAppName(appInstance.getAppName());
        thriftMessage.setEnv(appInstance.getEnv());
        thriftMessage.setHostName(appInstance.getHostIp());
        thriftMessage.setPort(appInstance.getPort());
        thriftMessage.setInterval(interval);
        thriftMessage.setTimestamp(timestamp);

        thriftMessage.setInstanceTimeEntity(new InstanceTimeEntity(metric.upTime, metric.startTime));
        thriftMessage.setCpuEntity(new CpuEntity(metric.cpuMetrics.processorNumber,
                                                 metric.cpuMetrics.processCpuTime,
                                                 metric.cpuMetrics.avgSystemLoad,
                                                 metric.cpuMetrics.processCpuLoad));
        thriftMessage.setMemoryEntity(new MemoryEntity(metric.memoryMetrics.allocatedBytes,
                                                       metric.memoryMetrics.freeBytes));
        thriftMessage.setHeapEntity(new HeapEntity(metric.heapMetrics.heapBytes,
                                                   metric.heapMetrics.heapInitBytes,
                                                   metric.heapMetrics.heapUsedBytes,
                                                   metric.heapMetrics.heapCommittedBytes));
        thriftMessage.setNonHeapEntity(new NonHeapEntity(metric.nonHeapMetrics.nonHeapBytes,
                                                         metric.nonHeapMetrics.nonHeapInitBytes,
                                                         metric.nonHeapMetrics.nonHeapUsedBytes,
                                                         metric.nonHeapMetrics.nonHeapCommitted));
        thriftMessage.setThreadEntity(new ThreadEntity(metric.threadMetrics.peakActiveCount,
                                                       metric.threadMetrics.activeDaemonCount,
                                                       metric.threadMetrics.totalCreatedCount,
                                                       metric.threadMetrics.activeThreadsCount));
        thriftMessage.setGcEntities(metric.gcMetrics.stream().map(gcEntity -> {
            GcEntity e = new GcEntity(gcEntity.gcCount,
                                      gcEntity.gcTime);
            e.setGcName(gcEntity.gcName);
            return e;
        }).collect(Collectors.toList()));

        thriftMessage.setClassesEntity(new ClassEntity(metric.classMetrics.currentClassCount,
                                                       metric.classMetrics.loadedClassCount,
                                                       metric.classMetrics.unloadedClassCount));
        thriftMessage.setMetaspaceEntity(new MetaspaceEntity(metric.metaspaceMetrics.metaspaceCommittedBytes,
                                                             metric.metaspaceMetrics.metaspaceUsedBytes));
        return thriftMessage;
    }

    @Override
    public Object from(AppInstance appInstance,
                       long timestamp,
                       int interval,
                       WebServerMetric metric) {
        WebServerMessage thriftMessage = new WebServerMessage();
        thriftMessage.setAppName(appInstance.getAppName());
        thriftMessage.setEnv(appInstance.getEnv());
        thriftMessage.setHostName(appInstance.getHostIp());
        thriftMessage.setPort(appInstance.getPort());
        thriftMessage.setInterval(interval);
        thriftMessage.setTimestamp(timestamp);
        thriftMessage.setServerEntity(new WebServerEntity(metric.getConnectionCount(),
                                                          metric.getMaxConnections(),
                                                          metric.getActiveThreads(),
                                                          metric.getMaxThreads(),
                                                          metric.getServerType().type()));
        return thriftMessage;
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
        RedisMessage thriftMessage = new RedisMessage();
        thriftMessage.setAppName(appInstance.getAppName());
        thriftMessage.setEnv(appInstance.getEnv());
        thriftMessage.setHostName(appInstance.getHostIp());
        thriftMessage.setPort(appInstance.getPort());
        thriftMessage.setInterval(interval);
        thriftMessage.setTimestamp(timestamp);

        RedisEntity entity = new RedisEntity();
        entity.setUri(metric.getHostAndPort());
        entity.setCommand(metric.getCommand());
        entity.setExceptionCount(metric.getExceptionCount());
        entity.setTotalCount(metric.getTotalCount());
        entity.setRequestTime(metric.getRequestTime());
        entity.setResponseTime(metric.getResponseTime());
        entity.setRequestBytes(metric.getRequestBytes());
        entity.setResponseBytes(metric.getResponseBytes());

        thriftMessage.setRedisList(Collections.singletonList(entity));
        return thriftMessage;
    }

    @Override
    public List<Object> from(AppInstance appInstance,
                             long timestamp,
                             int interval,
                             Collection<ExceptionMetric> counters) {
        ExceptionMessage thriftMessage = new ExceptionMessage();
        thriftMessage.setAppName(appInstance.getAppName());
        thriftMessage.setEnv(appInstance.getEnv());
        thriftMessage.setHostName(appInstance.getHostIp());
        thriftMessage.setPort(appInstance.getPort());
        thriftMessage.setInterval(interval);
        thriftMessage.setTimestamp(timestamp);

        thriftMessage.setExceptionList(counters.stream().map(counter -> {
            ExceptionEntity entity = new ExceptionEntity();
            entity.setUri(counter.getUri());
            entity.setMessage(counter.getMessage());
            entity.setClassName(counter.getExceptionClass());
            entity.setStackTrace(counter.getStackTrace());
            entity.setExceptionCount(counter.getCount());
            return entity;
        }).collect(Collectors.toList()));

        return Collections.singletonList(thriftMessage);
    }

    @Override
    public Object from(AppInstance appInstance, List<com.sbss.bithon.agent.core.tracing.context.TraceSpan> traceSpans) {
        TraceMessage thriftMessage = new TraceMessage();
        thriftMessage.setAppName(appInstance.getAppName());
        thriftMessage.setEnv(appInstance.getEnv());
        thriftMessage.setHostName(appInstance.getHostIp());
        thriftMessage.setPort(appInstance.getPort());
        thriftMessage.setTimestamp(System.currentTimeMillis());

        for (com.sbss.bithon.agent.core.tracing.context.TraceSpan span : traceSpans) {
            TraceSpan spanMessage = new TraceSpan();
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
            thriftMessage.addToSpans(spanMessage);
        }
        return thriftMessage;
    }

    @Override
    public Object from(AppInstance appInstance, EventMessage message) {
        ThriftEventMessage thriftMessage = new ThriftEventMessage();
        thriftMessage.setAppName(appInstance.getAppName());
        thriftMessage.setEnv(appInstance.getEnv());
        thriftMessage.setHostName(appInstance.getHostIp());
        thriftMessage.setPort(appInstance.getPort());
        thriftMessage.setTimestamp(System.currentTimeMillis());

        thriftMessage.setEventType(message.getMessageType());
        thriftMessage.setArguments(message.getArgs());
        return thriftMessage;
    }

    @Override
    public Object from(AppInstance appInstance,
                       long timestamp,
                       int interval,
                       List<AbstractThreadPoolMetrics> metricsList) {
        ThreadPoolMessage thriftMessage = new ThreadPoolMessage();
        thriftMessage.setAppName(appInstance.getAppName());
        thriftMessage.setEnv(appInstance.getEnv());
        thriftMessage.setHostName(appInstance.getHostIp());
        thriftMessage.setPort(appInstance.getPort());
        thriftMessage.setInterval(interval);
        thriftMessage.setTimestamp(timestamp);

        thriftMessage.setPools(metricsList.stream().map(metrics -> {
            ThreadPoolEntity entity = new ThreadPoolEntity();
            entity.setExecutorClass(metrics.getExecutorClass());
            entity.setPoolName(metrics.getThreadPoolName());

            entity.setActiveThreads(metrics.getActiveThreads());
            entity.setCurrentPoolSize(metrics.getCurrentPoolSize());
            entity.setMaxPoolSize(metrics.getMaxPoolSize());
            entity.setLargestPoolSize(metrics.getLargestPoolSize());
            entity.setQueuedTaskCount(metrics.getQueuedTaskCount());
            entity.setCallerRunTaskCount(metrics.getCallerRunTaskCount());
            entity.setAbortedTaskCount(metrics.getAbortedTaskCount());
            entity.setDiscardedTaskCount(metrics.getDiscardedTaskCount());
            entity.setDiscardedOldestTaskCount(metrics.getDiscardedOldestTaskCount());
            entity.setExceptionTaskCount(metrics.getExceptionTaskCount());
            entity.setSuccessfulTaskCount(metrics.getSuccessfulTaskCount());
            entity.setTotalTaskCount(metrics.getTotalTaskCount());

            return entity;
        }).collect(Collectors.toList()));

        return thriftMessage;
    }
}

