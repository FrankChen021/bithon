package com.sbss.bithon.agent.plugin.jvm;

import com.sbss.bithon.agent.core.metric.Delta;
import com.sbss.bithon.agent.core.metric.jvm.CpuMetric;

import java.util.concurrent.TimeUnit;

import static com.sbss.bithon.agent.plugin.jvm.JmxBeans.OS_BEAN;
import static com.sbss.bithon.agent.plugin.jvm.JmxBeans.RUNTIME_BEAN;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/14 8:33 下午
 */
public class CpuMetricCollector {
    private final Delta lastProcessCpuTime = new Delta(OS_BEAN.getProcessCpuTime());
    private final Delta lastProcessUpTime = new Delta(RUNTIME_BEAN.getUptime());

    /**
     * Calculate CPU usage by code instead of by JMX API
     * operatingSystemMXBean.getProcessCpuLoad()
     * https://bugs.java.com/bugdatabase/view_bug.do?bug_id=JDK-8226575
     * https://github.com/alibaba/Sentinel/pull/1204
     */
    public CpuMetric collect() {
        long processCpuTime = lastProcessCpuTime.update(OS_BEAN.getProcessCpuTime());
        long processUpTime = lastProcessUpTime.update(RUNTIME_BEAN.getUptime());
        long processCpuTimeDiffInMs = TimeUnit.NANOSECONDS.toMillis(processCpuTime);

        int cpuCores = OS_BEAN.getAvailableProcessors();
        double processCpuPercentage = (double) processCpuTimeDiffInMs / processUpTime / cpuCores * 100;

        return new CpuMetric(cpuCores,
                             processCpuTime,
                             OS_BEAN.getSystemLoadAverage(),
                             processCpuPercentage);
    }
}
