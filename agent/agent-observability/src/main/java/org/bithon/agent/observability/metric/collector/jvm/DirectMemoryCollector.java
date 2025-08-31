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

import org.bithon.agent.instrumentation.expt.AgentException;
import org.bithon.agent.java.adaptor.JavaAdaptorFactory;
import org.bithon.agent.observability.metric.domain.jvm.MemoryRegionMetrics;

import java.lang.management.BufferPoolMXBean;
import java.lang.management.ManagementFactory;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/7/9 22:14
 */
public class DirectMemoryCollector {

    private static final BufferPoolMXBean DIRECT_MEMORY_BEAN = ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class)
                                                                                .stream()
                                                                                .filter(bean -> "direct".equalsIgnoreCase(bean.getName()))
                                                                                .findFirst()
                                                                                .get();

    private final long max;

    DirectMemoryCollector() throws AgentException {
        max = JavaAdaptorFactory.getAdaptor().getMaxDirectMemory();
    }

    public MemoryRegionMetrics collect() {
        long used = DIRECT_MEMORY_BEAN.getMemoryUsed();
        return new MemoryRegionMetrics(max, 0, used, max - used);
    }
}
