package com.sbss.bithon.agent.core.metrics.jvm;

import java.io.Serializable;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/12/29 9:52 下午
 */
public class MemoryMetric implements Serializable {

    // memory in bytes that allocated to current application
    public long allocatedBytes;
    public long freeBytes;

    public MemoryMetric(long allocatedBytes, long freeBytes) {
        this.allocatedBytes = allocatedBytes;
        this.freeBytes = freeBytes;
    }
}
