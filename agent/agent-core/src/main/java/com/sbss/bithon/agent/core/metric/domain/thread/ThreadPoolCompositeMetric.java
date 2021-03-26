package com.sbss.bithon.agent.core.metric.domain.thread;

import com.sbss.bithon.agent.core.metric.model.Sum;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/25 10:48 下午
 */
public abstract class ThreadPoolCompositeMetric {

    private final String executorClass;
    private final String threadPoolName;

    public Sum callerRunTaskCount = new Sum();
    public Sum abortedTaskCount = new Sum();
    public Sum discardedTaskCount = new Sum();
    public Sum discardedOldestTaskCount = new Sum();
    public Sum exceptionTaskCount = new Sum();
    public Sum successfulTaskCount = new Sum();
    public Sum totalTaskCount = new Sum();

    public ThreadPoolCompositeMetric(String executorClass, String threadPoolName) {
        this.executorClass = executorClass;
        this.threadPoolName = threadPoolName;
    }

    public abstract long getActiveThreads();

    public abstract long getCurrentPoolSize();

    public abstract long getMaxPoolSize();

    public abstract long getLargestPoolSize();

    public abstract long getQueuedTaskCount();

    public long getCallerRunTaskCount() {
        return callerRunTaskCount.get();
    }

    public long getAbortedTaskCount() {
        return abortedTaskCount.get();
    }

    public long getDiscardedTaskCount() {
        return discardedTaskCount.get();
    }

    public long getDiscardedOldestTaskCount() {
        return discardedOldestTaskCount.get();
    }

    public long getExceptionTaskCount() {
        return exceptionTaskCount.get();
    }

    public long getSuccessfulTaskCount() {
        return successfulTaskCount.get();
    }

    public long getTotalTaskCount() {
        return totalTaskCount.get();
    }

    public String getExecutorClass() {
        return executorClass;
    }

    public String getThreadPoolName() {
        return threadPoolName;
    }
}
