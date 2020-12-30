package com.sbss.bithon.agent.plugin.jvm;

import com.sun.management.GcInfo;

import java.lang.management.GarbageCollectorMXBean;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GcInfoProvider {

    /**
     * GcName -> gcInfo
     */
    private static Map<String, GcInfo> lastGCInfo = new ConcurrentHashMap<>();

    public static Map<String, GcInfo> getLastGCInfo() {
        return lastGCInfo;
    }

    public static void setLastGCInfo(String gcName,
                                     GcInfo gcInfo) {
        lastGCInfo.put(gcName, gcInfo);
    }

    /**
     * GcName -> gcBean
     */
    private static Map<String, GarbageCollectorMXBean> gcBeans = new ConcurrentHashMap<>();

    public static Map<String, GarbageCollectorMXBean> getGcBeans() {
        return gcBeans;
    }

    public static void putGcBean(String gcName,
                                 GarbageCollectorMXBean gcBean) {
        gcBeans.put(gcName, gcBean);
    }

}
