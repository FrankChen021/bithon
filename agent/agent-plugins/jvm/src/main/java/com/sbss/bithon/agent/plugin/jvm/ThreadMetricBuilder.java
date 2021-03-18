package com.sbss.bithon.agent.plugin.jvm;

import com.sbss.bithon.agent.core.metric.jvm.ThreadMetric;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/14 8:27 下午
 */
public class ThreadMetricBuilder {
    public static ThreadMetric build() {
        final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        return new ThreadMetric(threadBean.getPeakThreadCount(),
                                threadBean.getDaemonThreadCount(),
                                threadBean.getTotalStartedThreadCount(),
                                threadBean.getThreadCount());

    }
}
