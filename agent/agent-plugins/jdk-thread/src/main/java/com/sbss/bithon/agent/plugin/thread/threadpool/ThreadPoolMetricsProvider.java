package com.sbss.bithon.agent.plugin.thread.threadpool;

import com.sbss.bithon.agent.core.context.AppInstance;
import com.sbss.bithon.agent.core.dispatcher.IMessageConverter;
import com.sbss.bithon.agent.core.metrics.IMetricProvider;
import com.sbss.bithon.agent.core.metrics.MetricProviderManager;
import com.sbss.bithon.agent.core.metrics.thread.AbstractThreadPoolMetrics;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/25 9:13 下午
 */
class ThreadPoolMetricsProvider implements IMetricProvider {
    private final Logger log = LoggerFactory.getLogger(ThreadPoolMetricsProvider.class);

    static ThreadPoolMetricsProvider INSTANCE;

    public static ThreadPoolMetricsProvider getInstance() {
        if (INSTANCE != null) {
            return INSTANCE;
        }
        synchronized (ThreadPoolMetricsProvider.class) {
            //double check
            if (INSTANCE != null) {
                return INSTANCE;
            }

            INSTANCE = new ThreadPoolMetricsProvider();
            MetricProviderManager.getInstance().register("threadpool", INSTANCE);
            return INSTANCE;
        }
    }

    private final Map<AbstractExecutorService, AbstractThreadPoolMetrics> executorMetrics = new ConcurrentHashMap<>();
    private final Queue<AbstractThreadPoolMetrics> flushed = new ConcurrentLinkedQueue<>();

    public void insertThreadPoolMetrics(AbstractExecutorService pool, AbstractThreadPoolMetrics metrics) {
        executorMetrics.put(pool, metrics);
    }

    public void deleteThreadPoolMetrics(AbstractExecutorService executor) {
        AbstractThreadPoolMetrics metrics = executorMetrics.remove(executor);
        if (metrics != null) {
            flushed.add(metrics);
        }
    }

    private Optional<AbstractThreadPoolMetrics> getMetrics(AbstractExecutorService executor) {
        return Optional.ofNullable(executorMetrics.get(executor));
    }

    public void addRunCount(AbstractExecutorService executor,
                            boolean hasException) {
        this.getMetrics(executor).ifPresent((metrics) -> {
            if (hasException) {
                metrics.exceptionTaskCount.incr();
            } else {
                metrics.successfulTaskCount.incr();
            }
            metrics.totalTaskCount.incr();
        });
    }

    public void addTotal(AbstractExecutorService pool) {
        this.getMetrics(pool).ifPresent((metrics) -> metrics.totalTaskCount.incr());
    }

    public void addAbort(ThreadPoolExecutor pool) {
        this.getMetrics(pool).ifPresent((metrics) -> metrics.abortedTaskCount.incr());
    }

    public void addCallerRun(ThreadPoolExecutor pool) {
        this.getMetrics(pool).ifPresent((metrics) -> metrics.callerRunTaskCount.incr());
    }

    public void addDiscard(ThreadPoolExecutor pool) {
        this.getMetrics(pool).ifPresent((metrics) -> metrics.discardedTaskCount.incr());
    }

    public void addDiscardOldest(ThreadPoolExecutor pool) {
        this.getMetrics(pool).ifPresent((metrics) -> metrics.discardedOldestTaskCount.incr());
    }

    @Override
    public boolean isEmpty() {
        return this.executorMetrics.isEmpty();
    }

    @Override
    public List<Object> buildMessages(IMessageConverter messageConverter,
                                      AppInstance appInstance,
                                      int interval,
                                      long timestamp) {
        List<AbstractThreadPoolMetrics> metricsList = new ArrayList<>();
        AbstractThreadPoolMetrics metrics = this.flushed.poll();
        while (metrics != null) {
            metricsList.add(metrics);
            metrics = this.flushed.poll();
        }
        metricsList.addAll(this.executorMetrics.values());
        Object message = messageConverter.from(appInstance,
                                               timestamp,
                                               interval,
                                               metricsList);
        return Collections.singletonList(message);
    }
}
