package com.sbss.bithon.agent.plugin.thread.threadpool;

import com.sbss.bithon.agent.core.metrics.thread.AbstractThreadPoolMetrics;
import com.sbss.bithon.agent.core.utils.ReflectionUtils;

import java.util.concurrent.ForkJoinPool;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/25 11:26 下午
 */
public class ForkJoinPoolMetrics extends AbstractThreadPoolMetrics {
    private final ForkJoinPool pool;
    private long largestPoolSize = 0;

    public ForkJoinPoolMetrics(ForkJoinPool pool) {
        super(pool.getClass().getName(),
              ThreadPoolUtils.stripSuffix((String) ReflectionUtils.getFieldValue(pool, "workerNamePrefix"), "-"));
        this.pool = pool;
    }

    @Override
    public long getActiveThreads() {
        return pool.getActiveThreadCount();
    }

    @Override
    public long getCurrentPoolSize() {
        return pool.getPoolSize();
    }

    @Override
    public long getMaxPoolSize() {
        return pool.getParallelism();
    }

    @Override
    public long getLargestPoolSize() {
        largestPoolSize = Math.max(pool.getPoolSize(), largestPoolSize);
        return largestPoolSize;
    }

    @Override
    public long getQueuedTaskCount() {
        return pool.getQueuedTaskCount();
    }
}
