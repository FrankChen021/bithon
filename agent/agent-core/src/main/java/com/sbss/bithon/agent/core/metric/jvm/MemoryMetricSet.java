package com.sbss.bithon.agent.core.metric.jvm;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/12/29 9:52 下午
 */
public class MemoryMetricSet {

    // memory in bytes that allocated to current application
    public long allocatedBytes;
    public long freeBytes;

    public MemoryMetricSet(long allocatedBytes, long freeBytes) {
        this.allocatedBytes = allocatedBytes;
        this.freeBytes = freeBytes;
    }
}
