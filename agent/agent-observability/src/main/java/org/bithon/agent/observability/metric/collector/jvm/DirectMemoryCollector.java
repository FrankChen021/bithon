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
import org.bithon.agent.observability.metric.domain.jvm.MemoryRegionMetrics;
import org.bithon.component.commons.utils.JdkUtils;

import java.lang.management.BufferPoolMXBean;
import java.lang.management.ManagementFactory;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/7/9 22:14
 */
public class DirectMemoryCollector {

    /**
     * Interface for collecting maximum direct memory information across different JDK versions.
     * This interface abstracts the JDK version-specific implementations to avoid reflection
     * and module access issues in JDK 9+.
     *
     * @author frank.chen021@outlook.com
     * @date 2024/12/19
     */
    public interface IMaxDirectMemoryGetter {

        /**
         * Gets the maximum amount of direct memory that can be allocated.
         *
         * @return the maximum direct memory in bytes, or -1 if the maximum is unlimited
         */
        long getMaxDirectMemory();
    }

    private static IMaxDirectMemoryGetter createCollector() {
        String implementation = (JdkUtils.MAJOR_VERSION >= 9)
            ? "org.bithon.agent.observability.metric.collector.jvm.MaxDirectMemoryCollectorJdk9"
            : "org.bithon.agent.observability.metric.collector.jvm.MaxDirectMemoryCollectorJdk8";

        try {
            Class<?> impl = Class.forName(implementation);
            return (IMaxDirectMemoryGetter) impl.getDeclaredConstructor().newInstance();
        } catch (Throwable e) {
            throw new AgentException("JDK " + JdkUtils.MAJOR_VERSION + " detected but no MaxDirectMemoryCollector implementation could be instantiated", e);
        }
    }

    private static final BufferPoolMXBean DIRECT_MEMORY_BEAN = ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class)
                                                                                .stream()
                                                                                .filter(bean -> "direct".equalsIgnoreCase(bean.getName()))
                                                                                .findFirst()
                                                                                .get();

    private final long max;

    DirectMemoryCollector() throws AgentException {
        IMaxDirectMemoryGetter maxDirectMemoryCollector = createCollector();
        max = maxDirectMemoryCollector.getMaxDirectMemory();
    }

    public MemoryRegionMetrics collect() {
        long used = DIRECT_MEMORY_BEAN.getMemoryUsed();
        return new MemoryRegionMetrics(max, 0, used, max - used);
    }
}
