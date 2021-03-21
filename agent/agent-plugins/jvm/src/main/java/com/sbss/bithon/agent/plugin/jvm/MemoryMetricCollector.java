package com.sbss.bithon.agent.plugin.jvm;

import com.sbss.bithon.agent.core.metric.jvm.HeapMetricSet;
import com.sbss.bithon.agent.core.metric.jvm.MemoryMetricSet;
import com.sbss.bithon.agent.core.metric.jvm.MetaspaceMetricSet;
import com.sbss.bithon.agent.core.metric.jvm.NonHeapMetricSet;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/14 8:21 下午
 */
public class MemoryMetricCollector {

    public static MemoryMetricSet buildMemoryMetrics() {
        return new MemoryMetricSet(Runtime.getRuntime().totalMemory(),
                                   Runtime.getRuntime().freeMemory());

    }

    public static HeapMetricSet collectHeap() {
        return new HeapMetricSet(JmxBeans.MEM_BEAN.getHeapMemoryUsage().getMax(),
                                 JmxBeans.MEM_BEAN.getHeapMemoryUsage().getInit(),
                                 JmxBeans.MEM_BEAN.getHeapMemoryUsage().getUsed(),
                                 JmxBeans.MEM_BEAN.getHeapMemoryUsage().getCommitted());

    }

    public static NonHeapMetricSet collectNonHeap() {
        return new NonHeapMetricSet(JmxBeans.MEM_BEAN.getNonHeapMemoryUsage().getMax(),
                                    JmxBeans.MEM_BEAN.getNonHeapMemoryUsage().getInit(),
                                    JmxBeans.MEM_BEAN.getNonHeapMemoryUsage().getUsed(),
                                    JmxBeans.MEM_BEAN.getNonHeapMemoryUsage()
                                                  .getCommitted());
    }

    public static MetaspaceMetricSet collectMeataSpace() {
        MetaspaceMetricSet metrics = new MetaspaceMetricSet();
        for (MemoryPoolMXBean bean : ManagementFactory.getMemoryPoolMXBeans()) {
            if ("Metaspace".equalsIgnoreCase(bean.getName())) {
                metrics.metaspaceCommittedBytes = bean.getUsage().getCommitted();
                metrics.metaspaceUsedBytes = bean.getUsage().getUsed();
            }
        }
        return metrics;
    }
}
