package com.sbss.bithon.agent.core.metric.jvm;

import java.io.Serializable;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/12/29 9:52 下午
 */
public class HeapMetric implements Serializable {
    // equal to value of -Xmx
    public long heapBytes;
    // equal to value of -Xms
    public long heapInitBytes;

    public long heapUsedBytes;

    /**
     * Returns the amount of memory in bytes that is committed for the Java virtual machine to use.
     * This amount of memory is guaranteed for the Java virtual machine to use.
     */
    public long heapCommittedBytes;

    public HeapMetric(long heapBytes, long heapInitBytes, long heapUsedBytes, long heapCommittedBytes) {
        this.heapBytes = heapBytes;
        this.heapInitBytes = heapInitBytes;
        this.heapUsedBytes = heapUsedBytes;
        this.heapCommittedBytes = heapCommittedBytes;
    }
}
