package com.sbss.bithon.agent.plugin.thread.threadpool;

import com.sbss.bithon.agent.core.metric.thread.ThreadPoolMetric;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/25 10:48 下午
 */
class ThreadPoolExecutorMetric extends ThreadPoolMetric {

    private final ThreadPoolExecutor executor;

    ThreadPoolExecutorMetric(ThreadPoolExecutor executor) {
        super(executor.getThreadFactory().getClass().getName(),
              ThreadPoolUtils.getThreadPoolName(executor.getThreadFactory()));
        this.executor = executor;
    }

    @Override
    public long getActiveThreads() {
        return executor.getActiveCount();
    }

    @Override
    public long getCurrentPoolSize() {
        return executor.getPoolSize();
    }

    @Override
    public long getMaxPoolSize() {
        return executor.getMaximumPoolSize();
    }

    @Override
    public long getLargestPoolSize() {
        return executor.getLargestPoolSize();
    }

    @Override
    public long getQueuedTaskCount() {
        return executor.getQueue().size();
    }
}
