package com.sbss.bithon.agent.plugin.thread.threadpool;

import com.sbss.bithon.agent.core.dispatcher.IMessageConverter;
import com.sbss.bithon.agent.core.metric.collector.IMetricCollector;
import com.sbss.bithon.agent.core.metric.collector.MetricCollectorManager;
import com.sbss.bithon.agent.core.metric.domain.thread.ThreadPoolCompositeMetric;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/25 9:13 下午
 */
class ThreadPoolMetricsCollector implements IMetricCollector {
    static ThreadPoolMetricsCollector INSTANCE;
    private final Map<AbstractExecutorService, ThreadPoolCompositeMetric> executorMetrics = new ConcurrentHashMap<>();
    private final Queue<ThreadPoolCompositeMetric> flushed = new ConcurrentLinkedQueue<>();

    public static ThreadPoolMetricsCollector getInstance() {
        // See MetricCollectorManager for more detail to find why there's such a check below
        if (MetricCollectorManager.getInstance() == null) {
            return null;
        }

        if (INSTANCE != null) {
            return INSTANCE;
        }
        synchronized (ThreadPoolMetricsCollector.class) {
            //double check
            if (INSTANCE != null) {
                return INSTANCE;
            }

            INSTANCE = MetricCollectorManager.getInstance().register("thread-pool", new ThreadPoolMetricsCollector());
            return INSTANCE;
        }
    }

    public void addThreadPool(AbstractExecutorService pool, ThreadPoolCompositeMetric metrics) {
        executorMetrics.put(pool, metrics);
    }

    public void deleteThreadPool(AbstractExecutorService executor) {
        ThreadPoolCompositeMetric metrics = executorMetrics.remove(executor);
        if (metrics != null) {
            flushed.add(metrics);
        }
    }

    private Optional<ThreadPoolCompositeMetric> getMetrics(AbstractExecutorService executor) {
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
    public List<Object> collect(IMessageConverter messageConverter,
                                int interval,
                                long timestamp) {
        List<Object> messageList = new ArrayList<>(flushed.size() + executorMetrics.size());

        ThreadPoolCompositeMetric flushedMetric = this.flushed.poll();
        while (flushedMetric != null) {
            messageList.add(messageConverter.from(timestamp,
                                                  interval,
                                                  flushedMetric));
            flushedMetric = this.flushed.poll();
        }

        for (ThreadPoolCompositeMetric threadPoolMetric : this.executorMetrics.values()) {
            messageList.add(messageConverter.from(timestamp,
                                                  interval,
                                                  threadPoolMetric));
        }

        return messageList;
    }
}
