package com.sbss.bithon.agent.plugin.jvm;

import com.sun.management.OperatingSystemMXBean;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;

/**
 * @author frankchen
 */
public class JmxBeans {

    public static final RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
    public static final OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
    public static final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();

}
