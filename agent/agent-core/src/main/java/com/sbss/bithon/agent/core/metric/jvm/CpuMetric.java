package com.sbss.bithon.agent.core.metric.jvm;

import java.io.Serializable;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/12/29 9:49 下午
 */
public class CpuMetric implements Serializable {
    public long processorNumber;

    // CPU Time in nano seconds
    public long processCpuTime;

    public double avgSystemLoad;

    // CPU usage (%)
    public double processCpuLoad;

    public CpuMetric(long processorNumber,
                     long processCpuTime,
                     double avgSystemLoad,
                     double processCpuLoad) {
        this.processorNumber = processorNumber;
        this.processCpuTime = processCpuTime;
        this.avgSystemLoad = avgSystemLoad;
        this.processCpuLoad = processCpuLoad;
    }
}