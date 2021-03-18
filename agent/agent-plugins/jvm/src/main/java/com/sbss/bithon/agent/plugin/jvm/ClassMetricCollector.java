package com.sbss.bithon.agent.plugin.jvm;

import com.sbss.bithon.agent.core.metric.jvm.ClassMetric;

import java.lang.management.ClassLoadingMXBean;
import java.lang.management.ManagementFactory;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/14 8:29 下午
 */
public class ClassMetricCollector {
    public static ClassMetric collect() {
        final ClassLoadingMXBean classLoadingMXBean = ManagementFactory.getClassLoadingMXBean();
        return new ClassMetric(classLoadingMXBean.getTotalLoadedClassCount(),
                               classLoadingMXBean.getLoadedClassCount(),
                               classLoadingMXBean.getUnloadedClassCount());

    }
}
