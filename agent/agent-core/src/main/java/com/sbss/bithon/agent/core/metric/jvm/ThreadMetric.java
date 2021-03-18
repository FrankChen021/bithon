package com.sbss.bithon.agent.core.metric.jvm;

import java.io.Serializable;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/12/29 9:56 下午
 */
public class ThreadMetric implements Serializable {

    public long peakActiveCount;
    public long activeDaemonCount;
    public long totalCreatedCount;

    /**
     * current count of active threads, including daemon and non-daemon threads
     */
    public long activeThreadsCount;

    public ThreadMetric(long peakActiveCount,
                        long activeDaemonCount,
                        long totalCreatedCount,
                        long activeThreadsCount) {
        this.peakActiveCount = peakActiveCount;
        this.activeDaemonCount = activeDaemonCount;
        this.totalCreatedCount = totalCreatedCount;
        this.activeThreadsCount = activeThreadsCount;
    }
}
