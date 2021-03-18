package com.sbss.bithon.agent.plugin.jvm;

import com.sbss.bithon.agent.core.metric.Delta;
import com.sbss.bithon.agent.core.metric.jvm.GcMetric;
import com.sun.management.GarbageCollectionNotificationInfo;

import javax.management.NotificationBroadcaster;
import javax.management.openmbean.CompositeData;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TODO: Separate Gc Metric from JVM Metric
 * @author frank.chen021@outlook.com
 * @date 2021/2/14 8:39 下午
 */
public class GcMetricCollector {

    /**
     * GcName -> gcBean
     */
    private final Map<String, GarbageCollectorMXBean> GC_BEANS = new ConcurrentHashMap<>();
    private final Map<String, GcMetricValue> gcMetricMap;

    public GcMetricCollector() {
        this.gcMetricMap = new HashMap<>();

        for (GarbageCollectorMXBean gcBean : ManagementFactory.getGarbageCollectorMXBeans()) {
            NotificationBroadcaster broadcaster = (NotificationBroadcaster) gcBean;
            broadcaster.addNotificationListener((notification, handback) -> {
                                                    if (GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION.equals(notification.getType())) {
                                                        GarbageCollectionNotificationInfo nInfo = GarbageCollectionNotificationInfo.from((CompositeData) notification
                                                            .getUserData());
                                                        GC_BEANS.put(nInfo.getGcName(), gcBean);
                                                    }
                                                },
                                                null,
                                                null);
        }
    }

    public List<GcMetric> build() {
        List<GcMetric> metrics = new ArrayList<>();
        for (Map.Entry<String, GarbageCollectorMXBean> gcBean : GC_BEANS.entrySet()) {
            if (null == gcBean) {
                continue;
            }
            String gcName = gcBean.getKey().replace(" ", "");

            GcMetricValue gcMetricValue = gcMetricMap.computeIfAbsent(gcName, k -> new GcMetricValue());
            long gcCount = gcMetricValue.gcCount.update(gcBean.getValue().getCollectionCount());
            long gcTime = gcMetricValue.gcTime.update(gcBean.getValue().getCollectionTime());

            if (gcCount > 0 && gcTime > 0) {
                metrics.add(new GcMetric(gcName, gcCount, gcTime));
            }
        }
        return metrics;
    }

    private static class GcMetricValue {
        final Delta gcCount = new Delta();
        final Delta gcTime = new Delta();
    }
}
