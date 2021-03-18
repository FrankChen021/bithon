package com.sbss.bithon.agent.core.metric.jvm;

import java.io.Serializable;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/12/29 9:58 下午
 */
public class GcMetric implements Serializable {
    public String gcName;

    /**
     * count of GC between two intervals
     */
    public long gcCount;

    /**
     * time of total GC between two intervals in milli seconds
     */
    public long gcTime;

    public GcMetric(String gcName, long gcCount, long gcTime) {
        this.gcName = gcName;
        this.gcCount = gcCount;
        this.gcTime = gcTime;
    }
}
