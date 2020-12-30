package com.sbss.bithon.agent.plugin.jvm;

import com.sbss.bithon.agent.core.metrics.jvm.HeapMetric;
import com.sbss.bithon.agent.core.metrics.jvm.MemoryMetric;
import com.sbss.bithon.agent.core.metrics.jvm.MetaspaceMetric;
import com.sbss.bithon.agent.core.metrics.jvm.NonHeapMetric;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/14 8:21 下午
 */
public class MemoryMetricsBuilder {

    public static MemoryMetric buildMemoryMetrics() {
        return new MemoryMetric(Runtime.getRuntime().totalMemory(),
                                Runtime.getRuntime().freeMemory());

    }

    public static HeapMetric buildHeapMetrics() {
        return new HeapMetric(JmxBeans.memoryBean.getHeapMemoryUsage().getMax(),
                              JmxBeans.memoryBean.getHeapMemoryUsage().getInit(),
                              JmxBeans.memoryBean.getHeapMemoryUsage().getUsed(),
                              JmxBeans.memoryBean.getHeapMemoryUsage().getCommitted());

    }

    public static NonHeapMetric buildNonHeapMetrics() {
        return new NonHeapMetric(JmxBeans.memoryBean.getNonHeapMemoryUsage().getMax(),
                                 JmxBeans.memoryBean.getNonHeapMemoryUsage().getInit(),
                                 JmxBeans.memoryBean.getNonHeapMemoryUsage().getUsed(),
                                 JmxBeans.memoryBean.getNonHeapMemoryUsage()
                                     .getCommitted());
    }

    public static MetaspaceMetric buildMetaspaceMetrics() {
        MetaspaceMetric metrics = new MetaspaceMetric();
        for (MemoryPoolMXBean bean : ManagementFactory.getMemoryPoolMXBeans()) {
            if ("Metaspace".equalsIgnoreCase(bean.getName())) {
                metrics.metaspaceCommittedBytes = bean.getUsage().getCommitted();
                metrics.metaspaceUsedBytes = bean.getUsage().getUsed();
            }
        }
        return metrics;
    }
}
