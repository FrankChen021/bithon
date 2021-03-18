package com.sbss.bithon.agent.plugin.jvm;

import com.sun.management.OperatingSystemMXBean;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;

/**
 * @author frankchen
 */
public class JmxBeans {

    public static final RuntimeMXBean RUNTIME_BEAN = ManagementFactory.getRuntimeMXBean();
    public static final OperatingSystemMXBean OS_BEAN = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
    public static final MemoryMXBean MEM_BEAN = ManagementFactory.getMemoryMXBean();

}
