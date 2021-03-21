package com.sbss.bithon.agent.core.metric.jvm;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/12/29 9:58 下午
 */
public class GcMetricSet {
    public String gcName;

    /**
     * 0 - NEW
     * 1 - OLD
     */
    public int generation;

    /**
     * count of GC between two intervals
     */
    public long gcCount;

    /**
     * time of total GC between two intervals in milli seconds
     */
    public long gcTime;

    public GcMetricSet(String gcName, int generation, long gcCount, long gcTime) {
        this.gcName = gcName;
        this.generation = generation;
        this.gcCount = gcCount;
        this.gcTime = gcTime;
    }
}
