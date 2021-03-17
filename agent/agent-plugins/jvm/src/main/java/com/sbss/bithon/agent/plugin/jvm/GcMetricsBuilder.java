package com.sbss.bithon.agent.plugin.jvm;

import com.sbss.bithon.agent.core.metrics.jvm.GcMetric;
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
 * @author frank.chen021@outlook.com
 * @date 2021/2/14 8:39 下午
 */
public class GcMetricsBuilder {

    /**
     * GcName -> gcBean
     */
    private final Map<String, GarbageCollectorMXBean> GC_BEANS = new ConcurrentHashMap<>();
    private final Map<String, GcInfo> gcUsageMap;
    public GcMetricsBuilder() {
        this.gcUsageMap = new HashMap<>();

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
        List<GcMetric> gcMetricList = new ArrayList<>();
        for (Map.Entry<String, GarbageCollectorMXBean> gcBean : GC_BEANS.entrySet()) {
            if (null == gcBean) {
                continue;
            }
            int currentGcCount = 0, realGcCount, lastGcCount;
            long currentGcTime = 0, realGcTime, lastGcTime;

            String gcName = gcBean.getKey().replace(" ", "");

            GcInfo gcInfo = gcUsageMap.computeIfAbsent(gcName, k -> new GcInfo());

            lastGcCount = gcInfo.getLastGcCount();
            lastGcTime = gcInfo.getLastGcTime();

            currentGcCount += gcBean.getValue().getCollectionCount();
            currentGcTime += gcBean.getValue().getCollectionTime();

            realGcCount = currentGcCount - lastGcCount;
            realGcTime = currentGcTime - lastGcTime;

            gcInfo.setLastGcCount(currentGcCount);
            gcInfo.setLastGcTime(currentGcTime);

            gcMetricList.add(new GcMetric(gcName, realGcCount, realGcTime));
        }
        return gcMetricList;
    }

    private static class GcInfo {
        private int lastGcCount = 0;
        private long lastGcTime = 0;

        int getLastGcCount() {
            return lastGcCount;
        }

        void setLastGcCount(int lastGcCount) {
            this.lastGcCount = lastGcCount;
        }

        long getLastGcTime() {
            return lastGcTime;
        }

        void setLastGcTime(long lastGcTime) {
            this.lastGcTime = lastGcTime;
        }
    }
}
