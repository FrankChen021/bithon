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

package com.sbss.bithon.agent.dispatcher.netty;

import cn.bithon.rpc.services.metrics.ExceptionMetricMessage;
import cn.bithon.rpc.services.metrics.HttpClientMetricMessage;
import cn.bithon.rpc.services.metrics.JdbcPoolMetricMessage;
import cn.bithon.rpc.services.metrics.JvmGcMetricMessage;
import cn.bithon.rpc.services.metrics.JvmMetricMessage;
import cn.bithon.rpc.services.metrics.RedisMetricMessage;
import cn.bithon.rpc.services.metrics.ThreadPoolMetricMessage;
import cn.bithon.rpc.services.metrics.WebRequestMetricMessage;
import cn.bithon.rpc.services.metrics.WebServerMetricMessage;
import com.sbss.bithon.agent.core.dispatcher.IMessageConverter;
import com.sbss.bithon.agent.core.event.EventMessage;
import com.sbss.bithon.agent.core.metric.domain.exception.ExceptionMetricSet;
import com.sbss.bithon.agent.core.metric.domain.http.HttpClientCompositeMetric;
import com.sbss.bithon.agent.core.metric.domain.jdbc.JdbcPoolMetricSet;
import com.sbss.bithon.agent.core.metric.domain.jvm.GcCompositeMetric;
import com.sbss.bithon.agent.core.metric.domain.jvm.JvmMetricSet;
import com.sbss.bithon.agent.core.metric.domain.mongo.MongoDbCompositeMetric;
import com.sbss.bithon.agent.core.metric.domain.redis.RedisClientCompositeMetric;
import com.sbss.bithon.agent.core.metric.domain.sql.SqlCompositeMetric;
import com.sbss.bithon.agent.core.metric.domain.sql.SqlStatementCompositeMetric;
import com.sbss.bithon.agent.core.metric.domain.thread.ThreadPoolCompositeMetric;
import com.sbss.bithon.agent.core.metric.domain.web.WebRequestCompositeMetric;
import com.sbss.bithon.agent.core.metric.domain.web.WebServerMetricSet;
import com.sbss.bithon.agent.core.tracing.context.TraceSpan;

import java.util.List;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/6/27 20:13
 */
public class NettyRpcMessageConverter implements IMessageConverter {
    @Override
    public Object from(long timestamp, int interval, List<String> dimensions, HttpClientCompositeMetric metric) {
        return HttpClientMetricMessage.newBuilder()
                                      .setTimestamp(timestamp)
                                      .setInterval(interval)
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
        return JdbcPoolMetricMessage.newBuilder()
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
    public Object from(long timestamp, int interval, List<String> dimensions, WebRequestCompositeMetric metric) {
        return WebRequestMetricMessage.newBuilder()
                                      .setTimestamp(timestamp)
                                      .setInterval(interval)
                                      .setSrcApplication(dimensions.get(0))
                                      .setUri(dimensions.get(1))
                                      .setResponseTime(metric.getResponseTime().getSum().get())
                                      .setMaxResponseTime(metric.getResponseTime().getMax().get())
                                      .setMinResponseTime(metric.getResponseTime().getMin().get())
                                      .setCallCount(metric.getRequestCount().get())
                                      .setErrorCount(metric.getErrorCount().get())
                                      .setCount4Xx(metric.getCount4xx().get())
                                      .setCount5Xx(metric.getCount5xx().get())
                                      .setRequestBytes(metric.getRequestBytes().get())
                                      .setResponseBytes(metric.getResponseBytes().get())
                                      .build();
    }

    @Override
    public Object from(long timestamp, int interval, JvmMetricSet metric) {
        JvmMetricMessage.Builder builder = JvmMetricMessage.newBuilder();
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
        return WebServerMetricMessage.newBuilder()
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
        return RedisMetricMessage.newBuilder()
                                 .setTimestamp(timestamp)
                                 .setInterval(interval)
                                 .setUri(dimensions.get(0))
                                 .setCommand(dimensions.get(1))
                                 .setRequestTime(metric.getRequestTime().getSum().get())
                                 .setResponseTime(metric.getResponseTime().getSum().get())
                                 .setTotalCount(metric.getCallCount())
                                 .setExceptionCount(metric.getExceptionCount())
                                 .setRequestBytes(metric.getRequestBytes())
                                 .setResponseBytes(metric.getResponseBytes())
                                 .build();
    }

    @Override
    public Object from(long timestamp, int interval, ExceptionMetricSet metric) {
        return ExceptionMetricMessage.newBuilder()
                                     .setTimestamp(timestamp)
                                     .setInterval(interval)
                                     .setUri(metric.getUri())
                                     .setMessage(metric.getMessage())
                                     .setExceptionCount(metric.getCount())
                                     .build();
    }

    @Override
    public Object from(long timestamp, int interval, ThreadPoolCompositeMetric metric) {
        return ThreadPoolMetricMessage.newBuilder().setTimestamp(timestamp)
                                      .setInterval(interval)
                                      .setExecutorClass(metric.getExecutorClass())
                                      .setPoolName(metric.getThreadPoolName())
                                      .setCallerRunTaskCount(metric.getCallerRunTaskCount())
                                      .setAbortedTaskCount(metric.getAbortedTaskCount())
                                      .setDiscardedOldestTaskCount(metric.getDiscardedOldestTaskCount())
                                      .setDiscardedTaskCount(metric.getDiscardedTaskCount())
                                      .setExceptionTaskCount(metric.getExceptionTaskCount())
                                      .setSuccessfulTaskCount(metric.getSuccessfulTaskCount())
                                      .setTotalTaskCount(metric.getTotalTaskCount())
                                      .build();
    }

    @Override
    public Object from(TraceSpan span) {
        return null;
    }

    @Override
    public Object from(EventMessage event) {
        return null;
    }

    @Override
    public Object from(Map<String, String> log) {
        return null;
    }

    @Override
    public Object from(long timestamp, int interval, GcCompositeMetric gcMetricSet) {
        return JvmGcMetricMessage.newBuilder().setTimestamp(timestamp)
                                 .setInterval(interval)
                                 .setGcName(gcMetricSet.getGcName())
                                 .setGeneration(gcMetricSet.getGeneration())
                                 .setGcTime(gcMetricSet.getGcTime())
                                 .setGcCount(gcMetricSet.getGcCount())
                                 .build();
    }
}
