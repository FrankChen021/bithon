package com.sbss.bithon.agent.core.metric.domain.jvm;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/12/29 9:52 下午
 */
public class MemoryCompositeMetric {

    // memory in bytes that allocated to current application
    public long allocatedBytes;
    public long freeBytes;

    public MemoryCompositeMetric(long allocatedBytes, long freeBytes) {
        this.allocatedBytes = allocatedBytes;
        this.freeBytes = freeBytes;
    }
}
