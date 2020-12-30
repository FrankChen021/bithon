package com.sbss.bithon.agent.plugin.jvm;

import com.keruyun.commons.agent.collector.entity.*;
import com.sbss.bithon.agent.core.interceptor.AbstractMethodIntercepted;
import com.sbss.bithon.agent.dispatcher.metrics.DispatchProcessor;
import com.sun.management.GarbageCollectionNotificationInfo;
import com.sun.management.UnixOperatingSystemMXBean;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import javax.management.NotificationBroadcaster;
import javax.management.NotificationListener;
import javax.management.openmbean.CompositeData;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.util.*;

public class JVMHandler extends AbstractMethodIntercepted {
    private static final Logger log = LoggerFactory.getLogger(JVMHandler.class);

    private static final String METASPACE = "Metaspace";
    private static final String CHECK_PERIOD = "checkPeriod";

    private DispatchProcessor dispatchProcessor;

    private boolean started = false;

    /**
     * 记录上一次获取到的cpuProcessTime
     */
    private long lastCpuTime;

    /**
     * 记录上一次取cpuProcessTime的 SystemTime, 作为锚点, 以用于精确计算数据发送时的interval, 最后精确到秒(int,
     * 无小数点)
     */
    private long lastAnchorTime;

    private class GcUsageTemp {
        /**
         * 记录上一次获取到的gcCount总值
         */
        private int lastGcCount = 0;

        /**
         * 记录上一次获取到的gcTime总值
         */
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

    private Map<String, GcUsageTemp> gcUsageTempMap = new HashMap<>();

    @Override
    public boolean init() throws Exception {
        int checkPeriod = 10;

        dispatchProcessor = DispatchProcessor.getInstance();
        for (GarbageCollectorMXBean bean : MXBeanProvider.garbageCollectorMXBeans) {
            NotificationBroadcaster broadcaster = (NotificationBroadcaster) bean;
            NotificationListener listener = (notification,
                                             handback) -> {
                if (GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION.equals(notification.getType())) {
                    GarbageCollectionNotificationInfo nInfo = GarbageCollectionNotificationInfo.from((CompositeData) notification.getUserData());
                    GcInfoProvider.setLastGCInfo(nInfo.getGcName(), nInfo.getGcInfo());
                    GcInfoProvider.putGcBean(nInfo.getGcName(), bean);
                }
            };
            broadcaster.addNotificationListener(listener, null, null);
        }

        // 首次记录cpuProcessTime&systemTime以用于计算cpu执行时间的时间间隔
        lastAnchorTime = System.nanoTime();

        lastCpuTime = MXBeanProvider.operatingSystemMXBean.getProcessCpuTime();

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                dispatch();
            }
        }, checkPeriod * 1000, checkPeriod * 1000);

        return true;
    }

    private InstanceInfoEntity buildEntity() {
        // 计算时间间隔 & CpuProcessTime在一个时间间隔内的差值,即真实使用时间
        long currentProcessCpuTime = MXBeanProvider.operatingSystemMXBean.getProcessCpuTime();
        long currentSystemTime = System.nanoTime();

        long realCpuProcessTime = currentProcessCpuTime - lastCpuTime;
        int realInterval = Math.round((float) (currentSystemTime - lastAnchorTime) / (1000 * 1000 * 1000));

        lastCpuTime = currentProcessCpuTime;
        lastAnchorTime = currentSystemTime;

        InstanceTimeEntity timeEntity = new InstanceTimeEntity(MXBeanProvider.runtimeMXBean.getUptime(),
                                                               MXBeanProvider.runtimeMXBean.getStartTime());
        CpuEntity cpuEntity = new CpuEntity(MXBeanProvider.operatingSystemMXBean.getAvailableProcessors(),
                                            MXBeanProvider.operatingSystemMXBean.getSystemLoadAverage(),
                                            realCpuProcessTime,
                                            MXBeanProvider.operatingSystemMXBean.getProcessCpuLoad() * 100);
        MemoryEntity memoryEntity = new MemoryEntity(Runtime.getRuntime().totalMemory(),
                                                     Runtime.getRuntime().freeMemory());
        HeapEntity heapEntity = new HeapEntity(MXBeanProvider.memoryMXBean.getHeapMemoryUsage().getMax(),
                                               MXBeanProvider.memoryMXBean.getHeapMemoryUsage().getInit(),
                                               MXBeanProvider.memoryMXBean.getHeapMemoryUsage().getUsed(),
                                               MXBeanProvider.memoryMXBean.getHeapMemoryUsage().getCommitted());
        NonHeapEntity nonHeapEntity = new NonHeapEntity(MXBeanProvider.memoryMXBean.getNonHeapMemoryUsage().getMax(),
                                                        MXBeanProvider.memoryMXBean.getNonHeapMemoryUsage().getInit(),
                                                        MXBeanProvider.memoryMXBean.getNonHeapMemoryUsage().getUsed(),
                                                        MXBeanProvider.memoryMXBean.getNonHeapMemoryUsage()
                                                            .getCommitted());
        MetaspaceEntity metaspaceEntity = new MetaspaceEntity(-1, -1);
        for (MemoryPoolMXBean bean : MXBeanProvider.memoryPoolMXBeans) {
            if (METASPACE.toLowerCase().equals(bean.getName().toLowerCase())) {
                metaspaceEntity.setMetaspaceCommitted(bean.getUsage().getCommitted());
                metaspaceEntity.setMetaspaceUsed(bean.getUsage().getUsed());
            }
        }

        GcEntity gcEntity = new GcEntity();
        List<GcUsage> gcUsageList = new ArrayList<>();
//        Map<String, GcInfo> gcInfoMap = GcInfoProvider.getLastGCInfo();
        Map<String, GarbageCollectorMXBean> gcBeans = GcInfoProvider.getGcBeans();
        for (Map.Entry<String, GarbageCollectorMXBean> gcBean : gcBeans.entrySet()) {
            if (null != gcBean) {
                // gcCount 当前总值 & gcCount差值 & 旧值
                int currentGcCount = 0, realGcCount, lastGcCount;
                // gcTime 当前总值 & gcTime & 旧值
                long currentGcTime = 0, realGcTime, lastGcTime;

                String gcName = gcBean.getKey().replace(" ", "");

                // 尝试从gcUsageTempMap获取上一次拿到的值, 如果没有就新建
                GcUsageTemp gcUsageTemp = gcUsageTempMap.computeIfAbsent(gcName, k -> new GcUsageTemp());

                lastGcCount = gcUsageTemp.getLastGcCount();
                lastGcTime = gcUsageTemp.getLastGcTime();

                currentGcCount += gcBean.getValue().getCollectionCount();
                currentGcTime += gcBean.getValue().getCollectionTime();

                realGcCount = currentGcCount - lastGcCount;
                realGcTime = currentGcTime - lastGcTime;

                gcUsageTemp.setLastGcCount(currentGcCount);
                gcUsageTemp.setLastGcTime(currentGcTime);

                GcUsage gcUsage = new GcUsage(realGcCount, realGcTime);
                gcUsage.setGcName(gcName);
                gcUsageList.add(gcUsage);

//                GcInfo gcInfoValue = gcInfo.getValue();
//                Map<String, GcUsage> usageBeforeGc = new HashMap<>(gcInfoValue.getMemoryUsageBeforeGc().size());
//                Map<String, GcUsage> usageAfterGc = new HashMap<>(gcInfoValue.getMemoryUsageAfterGc().size());
//                gcInfoValue.getMemoryUsageBeforeGc().keySet().forEach(k -> {
//                    MemoryUsage memoryUsage = gcInfoValue.getMemoryUsageBeforeGc().get(k);
//                    usageBeforeGc.put(k, new GcUsage(memoryUsage.getInit(), memoryUsage.getUsed(), memoryUsage.getCommitted(), memoryUsage.getMax()));
//                });
//                gcInfoValue.getMemoryUsageAfterGc().keySet().forEach(k -> {
//                    MemoryUsage memoryUsage = gcInfoValue.getMemoryUsageAfterGc().get(k);
//                    usageAfterGc.put(k, new GcUsage(memoryUsage.getInit(), memoryUsage.getUsed(), memoryUsage.getCommitted(), memoryUsage.getMax()));
//                });
//                GcEntity gcEntity = new GcEntity(gcName, gcInfoValue.getStartTime(), gcInfoValue.getEndTime(), usageBeforeGc, usageAfterGc);
            }
        }

        gcEntity.setGcUsageList(gcUsageList);

        ThreadEntity threadEntity = new ThreadEntity(MXBeanProvider.threadMXBean.getPeakThreadCount(),
                                                     MXBeanProvider.threadMXBean.getDaemonThreadCount(),
                                                     MXBeanProvider.threadMXBean.getTotalStartedThreadCount(),
                                                     MXBeanProvider.threadMXBean.getThreadCount());
        ClassesEntity classesEntity = new ClassesEntity(MXBeanProvider.classLoadingMXBean.getTotalLoadedClassCount(),
                                                        MXBeanProvider.classLoadingMXBean.getLoadedClassCount(),
                                                        MXBeanProvider.classLoadingMXBean.getUnloadedClassCount());
        return new InstanceInfoEntity(dispatchProcessor.getAppName(),
                                      dispatchProcessor.getIpAddress(),
                                      dispatchProcessor.getPort(),
                                      System.currentTimeMillis(),
                                      realInterval,
                                      null,
                                      timeEntity,
                                      cpuEntity,
                                      memoryEntity,
                                      heapEntity,
                                      nonHeapEntity,
                                      gcEntity,
                                      threadEntity,
                                      classesEntity,
                                      metaspaceEntity);
    }

    private void dispatch() {
        try {
            if (dispatchProcessor.ready) {
                dispatchProcessor.pushMessage(buildEntity());
                if (!started) {
                    dispatchStartInfo();
                    started = true;
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private void dispatchStartInfo() {
        dispatchProcessor.pushMessage(buildStartInfoEntity());
    }

    private DetailEntity buildStartInfoEntity() {
        Map<String, String> jvmDetail = new HashMap<>();
        Map<String, String> systemDetail = new HashMap<>();

        systemDetail.put("appName", dispatchProcessor.getAppName());
        systemDetail.put("hostName", dispatchProcessor.getIpAddress());
        systemDetail.put("port", String.valueOf(dispatchProcessor.getPort()));
        systemDetail.put("processors", String.valueOf(MXBeanProvider.operatingSystemMXBean.getAvailableProcessors()));
        systemDetail.put("osArch", MXBeanProvider.operatingSystemMXBean.getArch());
        systemDetail.put("osVersion", MXBeanProvider.operatingSystemMXBean.getVersion());
        systemDetail.put("osName", MXBeanProvider.operatingSystemMXBean.getName());

        jvmDetail.put("bootClassPath", MXBeanProvider.runtimeMXBean.getBootClassPath());
        jvmDetail.put("classPath", MXBeanProvider.runtimeMXBean.getClassPath());
        jvmDetail.put("jvmArgument", MXBeanProvider.runtimeMXBean.getInputArguments().toString());
        jvmDetail.put("libraryPath", MXBeanProvider.runtimeMXBean.getLibraryPath());
        jvmDetail.put("systemProperties", MXBeanProvider.runtimeMXBean.getSystemProperties().toString());
        jvmDetail.put("managementSpecVersion", MXBeanProvider.runtimeMXBean.getManagementSpecVersion());
        jvmDetail.put("runningJvmName", MXBeanProvider.runtimeMXBean.getName());
        jvmDetail.put("javaName", MXBeanProvider.runtimeMXBean.getSpecName());
        jvmDetail.put("javaVendor", MXBeanProvider.runtimeMXBean.getSpecVendor());
        jvmDetail.put("javaVersion", MXBeanProvider.runtimeMXBean.getSpecVersion());
        jvmDetail.put("javaVmName", MXBeanProvider.runtimeMXBean.getVmName());
        jvmDetail.put("javaVmVendor", MXBeanProvider.runtimeMXBean.getVmVendor());
        jvmDetail.put("javaVmVersion", MXBeanProvider.runtimeMXBean.getVmVersion());
        jvmDetail.put("heapInitial", String.valueOf(MXBeanProvider.memoryMXBean.getHeapMemoryUsage().getInit()));
        jvmDetail.put("heapMax", String.valueOf(MXBeanProvider.memoryMXBean.getHeapMemoryUsage().getMax()));
        jvmDetail.put("committedVirtualMemorySize",
                      String.valueOf(MXBeanProvider.operatingSystemMXBean.getCommittedVirtualMemorySize()));
        jvmDetail.put("totalPhysicalMemorySize",
                      String.valueOf(MXBeanProvider.operatingSystemMXBean.getTotalPhysicalMemorySize()));
        jvmDetail.put("totalSwapSpaceSize",
                      String.valueOf(MXBeanProvider.operatingSystemMXBean.getTotalSwapSpaceSize()));

        if (MXBeanProvider.operatingSystemMXBean instanceof UnixOperatingSystemMXBean) {
            UnixOperatingSystemMXBean unixOperatingSystemMXBean = (UnixOperatingSystemMXBean) MXBeanProvider.operatingSystemMXBean;
            jvmDetail.put("maxFileDescriptorCount",
                          String.valueOf(unixOperatingSystemMXBean.getMaxFileDescriptorCount()));
        }

        jvmDetail.put("startTime", String.valueOf(MXBeanProvider.runtimeMXBean.getStartTime()));

        return new DetailEntity(jvmDetail, systemDetail);
    }
}
