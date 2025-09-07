/*
 *    Copyright 2020 bithon.org
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

package org.bithon.agent.observability.metric.collector.jvm;

import org.bithon.agent.observability.metric.domain.jvm.MemoryMetrics;
import org.bithon.agent.observability.metric.domain.jvm.MemoryRegionMetrics;

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

    private static volatile DirectMemoryCollector directMemoryCollector;

    public static MemoryMetrics collectTotal() {
        return new MemoryMetrics(Runtime.getRuntime().totalMemory(),
                                 Runtime.getRuntime().freeMemory());
    }

    public static MemoryRegionMetrics collectHeap() {
        return new MemoryRegionMetrics(JmxBeans.MEM_BEAN.getHeapMemoryUsage());
    }

    public static MemoryRegionMetrics collectNonHeap() {
        return new MemoryRegionMetrics(JmxBeans.MEM_BEAN.getNonHeapMemoryUsage());
    }

    public static MemoryRegionMetrics collectMetaSpace() {
        return new MemoryRegionMetrics(META_SPACE_BEAN.getUsage());
    }

    public static MemoryRegionMetrics collectDirectMemory() {
        if (directMemoryCollector == null) {
            synchronized (MemoryMetricCollector.class) {
                if (directMemoryCollector == null) {
                    directMemoryCollector = new DirectMemoryCollector();
                }
            }
        }
        return directMemoryCollector.collect();
    }
}
