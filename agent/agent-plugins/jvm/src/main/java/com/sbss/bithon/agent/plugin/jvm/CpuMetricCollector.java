package com.sbss.bithon.agent.plugin.jvm;

import com.sbss.bithon.agent.core.metric.model.Delta;
import com.sbss.bithon.agent.core.metric.domain.jvm.CpuCompositeMetric;

import java.util.concurrent.TimeUnit;

import static com.sbss.bithon.agent.plugin.jvm.JmxBeans.OS_BEAN;
import static com.sbss.bithon.agent.plugin.jvm.JmxBeans.RUNTIME_BEAN;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/14 8:33 下午
 */
public class CpuMetricCollector {
    /**
     * in NANO-seconds
     */
    private final Delta processCpuTime = new Delta(OS_BEAN.getProcessCpuTime());

    /**
     * in milli-seconds
     */
    private final Delta processUpTime = new Delta(RUNTIME_BEAN.getUptime());

    /**
     * Calculate CPU usage by code instead of by JMX API
     * operatingSystemMXBean.getProcessCpuLoad()
     * https://bugs.java.com/bugdatabase/view_bug.do?bug_id=JDK-8226575
     * https://github.com/alibaba/Sentinel/pull/1204
     */
    public CpuCompositeMetric collect() {
        long processCpuTimeDelta = processCpuTime.update(OS_BEAN.getProcessCpuTime());
        long processUpTimeDelta = processUpTime.update(RUNTIME_BEAN.getUptime());
        int cpuCores = OS_BEAN.getAvailableProcessors();

        double processCpuPercentage = (double) TimeUnit.NANOSECONDS.toMillis(processCpuTimeDelta)
                                      / processUpTimeDelta
                                      / cpuCores * 100;

        return new CpuCompositeMetric(cpuCores,
                                processCpuTimeDelta,
                                OS_BEAN.getSystemLoadAverage(),
                                processCpuPercentage);
    }
}
