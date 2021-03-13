package com.sbss.bithon.agent.core.metrics.thread;

import com.sbss.bithon.agent.core.metrics.Counter;
import com.sbss.bithon.agent.core.metrics.Gauge;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/25 10:48 下午
 */
public abstract class ThreadPoolMetrics {

    private final String executorClass;
    private final String threadPoolName;

    public Counter callerRunTaskCount = new Counter();
    public Counter abortedTaskCount = new Counter();
    public Counter discardedTaskCount = new Counter();
    public Counter discardedOldestTaskCount = new Counter();
    public Counter exceptionTaskCount = new Counter();
    public Counter successfulTaskCount = new Counter();
    public Counter totalTaskCount = new Counter();

    public ThreadPoolMetrics(String executorClass, String threadPoolName) {
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
