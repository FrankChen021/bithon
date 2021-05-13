/*
 *    Copyright 2020 bithon.cn
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.sbss.bithon.agent.plugin.jvm;

import com.sbss.bithon.agent.core.metric.domain.jvm.MemoryCompositeMetric;
import com.sbss.bithon.agent.core.metric.domain.jvm.MemoryRegionCompositeMetric;
import sun.misc.VM;

import java.lang.management.BufferPoolMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/14 8:21 下午
 */
public class MemoryMetricCollector {

    private static final MemoryPoolMXBean META_SPACE_BEAN = ManagementFactory.getMemoryPoolMXBeans()
                                                                             .stream()
                                                                             .filter(bean -> "Metaspace".equalsIgnoreCase(bean.getName()))
                                                                             .findFirst()
                                                                             .get();

    private static final BufferPoolMXBean DIRECT_MEMORY_BEAN = ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class)
                                                                                .stream()
                                                                                .filter(bean -> "direct".equalsIgnoreCase(bean.getName()))
                                                                                .findFirst()
                                                                                .get();

    public static MemoryCompositeMetric buildMemoryMetrics() {
        return new MemoryCompositeMetric(Runtime.getRuntime().totalMemory(),
                                         Runtime.getRuntime().freeMemory());

    }

    public static MemoryRegionCompositeMetric collectHeap() {
        return new MemoryRegionCompositeMetric(JmxBeans.MEM_BEAN.getHeapMemoryUsage());

    }

    public static MemoryRegionCompositeMetric collectNonHeap() {
        return new MemoryRegionCompositeMetric(JmxBeans.MEM_BEAN.getNonHeapMemoryUsage());
    }

    public static MemoryRegionCompositeMetric collectMetaSpace() {
        return new MemoryRegionCompositeMetric(META_SPACE_BEAN.getUsage());
    }

    public static MemoryRegionCompositeMetric collectDirectMemory() {
        long max = VM.maxDirectMemory();
        long used = DIRECT_MEMORY_BEAN.getMemoryUsed();
        return new MemoryRegionCompositeMetric(max, 0, used, max - used);
    }
}
