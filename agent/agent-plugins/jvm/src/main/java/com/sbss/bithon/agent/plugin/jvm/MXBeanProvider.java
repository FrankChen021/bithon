package com.sbss.bithon.agent.plugin.jvm;

import com.sun.management.OperatingSystemMXBean;

import java.lang.management.*;
import java.util.List;

public class MXBeanProvider {

    public static final RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
    public static final OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
    public static final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
    public static final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
    public static final ClassLoadingMXBean classLoadingMXBean = ManagementFactory.getClassLoadingMXBean();
    public static final List<GarbageCollectorMXBean> garbageCollectorMXBeans = ManagementFactory.getGarbageCollectorMXBeans();
    public static final List<MemoryPoolMXBean> memoryPoolMXBeans = ManagementFactory.getMemoryPoolMXBeans();
}
