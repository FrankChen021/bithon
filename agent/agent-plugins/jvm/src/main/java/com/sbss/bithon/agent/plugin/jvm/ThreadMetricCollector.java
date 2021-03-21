package com.sbss.bithon.agent.plugin.jvm;

import com.sbss.bithon.agent.core.metric.jvm.ThreadMetricSet;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/14 8:27 下午
 */
public class ThreadMetricCollector {

    public static ThreadMetricSet collect() {
        final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        return new ThreadMetricSet(threadBean.getPeakThreadCount(),
                                   threadBean.getDaemonThreadCount(),
                                   threadBean.getTotalStartedThreadCount(),
                                   threadBean.getThreadCount());

    }
}
