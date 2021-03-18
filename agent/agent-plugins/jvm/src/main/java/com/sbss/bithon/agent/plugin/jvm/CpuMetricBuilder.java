package com.sbss.bithon.agent.plugin.jvm;

import com.sbss.bithon.agent.core.metric.jvm.CpuMetric;

import java.util.concurrent.TimeUnit;

import static com.sbss.bithon.agent.plugin.jvm.JmxBeans.osBean;
import static com.sbss.bithon.agent.plugin.jvm.JmxBeans.runtimeBean;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/14 8:33 下午
 */
public class CpuMetricBuilder {
    private long lastProcessCpuTime;
    private long lastProcessUpTime;

    public CpuMetricBuilder() {
        lastProcessCpuTime = osBean.getProcessCpuTime();
        lastProcessUpTime = runtimeBean.getUptime();
    }

    public CpuMetric build() {
        long currentProcessUpTime = runtimeBean.getUptime();
        long currentProcessCpuTime = osBean.getProcessCpuTime();

        int cpuCores = osBean.getAvailableProcessors();

        //
        // Calculate CPU usage by code instead of by JMX API
        // operatingSystemMXBean.getProcessCpuLoad()
        //      https://bugs.java.com/bugdatabase/view_bug.do?bug_id=JDK-8226575
        //      https://github.com/alibaba/Sentinel/pull/1204
        //
        long realCpuProcessTime = currentProcessCpuTime - lastProcessCpuTime;
        long processCpuTimeDiffInMs = TimeUnit.NANOSECONDS.toMillis(realCpuProcessTime);
        long processUpTimeDiffInMs = currentProcessUpTime - lastProcessUpTime;
        double processCpuPercentage = (double) processCpuTimeDiffInMs / processUpTimeDiffInMs / cpuCores * 100;

        lastProcessCpuTime = currentProcessCpuTime;
        lastProcessUpTime = currentProcessUpTime;

        return new CpuMetric(cpuCores,
                             realCpuProcessTime,
                             osBean.getSystemLoadAverage(),
                             processCpuPercentage);
    }
}
