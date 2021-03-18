package com.sbss.bithon.agent.plugin.jvm;

import com.sbss.bithon.agent.core.metric.jvm.HeapMetric;
import com.sbss.bithon.agent.core.metric.jvm.MemoryMetric;
import com.sbss.bithon.agent.core.metric.jvm.MetaspaceMetric;
import com.sbss.bithon.agent.core.metric.jvm.NonHeapMetric;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/14 8:21 下午
 */
public class MemoryMetricCollector {

    public static MemoryMetric buildMemoryMetrics() {
        return new MemoryMetric(Runtime.getRuntime().totalMemory(),
                                Runtime.getRuntime().freeMemory());

    }

    public static HeapMetric collectHeap() {
        return new HeapMetric(JmxBeans.MEM_BEAN.getHeapMemoryUsage().getMax(),
                              JmxBeans.MEM_BEAN.getHeapMemoryUsage().getInit(),
                              JmxBeans.MEM_BEAN.getHeapMemoryUsage().getUsed(),
                              JmxBeans.MEM_BEAN.getHeapMemoryUsage().getCommitted());

    }

    public static NonHeapMetric collectNonHeap() {
        return new NonHeapMetric(JmxBeans.MEM_BEAN.getNonHeapMemoryUsage().getMax(),
                                 JmxBeans.MEM_BEAN.getNonHeapMemoryUsage().getInit(),
                                 JmxBeans.MEM_BEAN.getNonHeapMemoryUsage().getUsed(),
                                 JmxBeans.MEM_BEAN.getNonHeapMemoryUsage()
                                                  .getCommitted());
    }

    public static MetaspaceMetric collectMeataSpace() {
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
