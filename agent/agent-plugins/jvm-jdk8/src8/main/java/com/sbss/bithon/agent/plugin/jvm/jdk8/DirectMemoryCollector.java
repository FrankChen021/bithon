package com.sbss.bithon.agent.plugin.jvm.jdk8;

import com.sbss.bithon.agent.core.metric.domain.jvm.MemoryRegionCompositeMetric;
import com.sbss.bithon.agent.plugin.jvm.IDirectMemoryCollector;
import sun.misc.VM;

import java.lang.management.BufferPoolMXBean;
import java.lang.management.ManagementFactory;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/7/9 22:14
 */
public class DirectMemoryCollector implements IDirectMemoryCollector {

    private static final BufferPoolMXBean DIRECT_MEMORY_BEAN = ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class)
                                                                                .stream()
                                                                                .filter(bean -> "direct".equalsIgnoreCase(bean.getName()))
                                                                                .findFirst()
                                                                                .get();

    @Override
    public MemoryRegionCompositeMetric collect() {
        long max = VM.maxDirectMemory();
        long used = DIRECT_MEMORY_BEAN.getMemoryUsed();
        return new MemoryRegionCompositeMetric(max, 0, used, max - used);
    }
}
