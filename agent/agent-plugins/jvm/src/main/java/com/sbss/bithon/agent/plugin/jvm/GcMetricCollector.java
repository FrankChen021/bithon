package com.sbss.bithon.agent.plugin.jvm;

import com.sbss.bithon.agent.core.metric.Delta;
import com.sbss.bithon.agent.core.metric.jvm.GcMetric;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TODO: Separate Gc Metric from JVM Metric
 *
 * @author frank.chen021@outlook.com
 * @date 2021/2/14 8:39 下午
 */
public class GcMetricCollector {

    static class GarbageCollector {
        private final GarbageCollectorMXBean gcBean;
        private final Delta gcCount = new Delta();
        private final Delta gcTime = new Delta();
        private final int generation;

        GarbageCollector(GarbageCollectorMXBean bean) {
            this.gcBean = bean;
            this.generation = getGeneration(bean.getName());
        }

        public GcMetric collect() {
            long gcCount = this.gcCount.update(gcBean.getCollectionCount());
            long gcTime = this.gcTime.update(gcBean.getCollectionTime());

            if (gcCount > 0 && gcTime > 0) {
                return new GcMetric(gcBean.getName(), generation, gcCount, gcTime);
            }
            return null;
        }

        private int getGeneration(String gcName) {
            switch (gcName) {
                // CMS
                case "ParNew":
                    return 0;
                case "ConcurrentMarkSweep":
                    return 1;

                // G1
                case "G1 Young Generation":
                    return 0;
                case "G1 Old Generation":
                    return 1;

                // Parallel
                case "PS Scavenge":
                    return 0;
                case "PS MarkSweep":
                    return 1;

                // Serial
                case "Copy":
                    return 0;
                case "MarkSweepCompact":
                    return 1;
                default:
                    return -1;
            }
        }
    }

    private final Map<String, GarbageCollector> collectors = new HashMap<>();

    public GcMetricCollector() {
        for (GarbageCollectorMXBean gcBean : ManagementFactory.getGarbageCollectorMXBeans()) {
            collectors.put(gcBean.getName(), new GarbageCollector(gcBean));
        }
    }

    public List<GcMetric> collect() {
        List<GcMetric> metrics = new ArrayList<>(collectors.size());
        for (GarbageCollector gcBean : collectors.values()) {

            GcMetric metric = gcBean.collect();
            if (metric != null) {
                metrics.add(metric);
            }
        }
        return metrics;
    }

}
