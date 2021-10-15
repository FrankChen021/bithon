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

package org.bithon.agent.plugin.jvm.mem;

import org.bithon.agent.bootstrap.expt.AgentException;
import org.bithon.agent.core.metric.domain.jvm.MemoryCompositeMetric;
import org.bithon.agent.core.metric.domain.jvm.MemoryRegionCompositeMetric;
import org.bithon.agent.core.plugin.PluginClassLoaderManager;
import org.bithon.agent.plugin.jvm.JmxBeans;
import shaded.org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/14 8:21 下午
 */
public class MemoryMetricCollector {

    private static final MemoryPoolMXBean META_SPACE_BEAN = ManagementFactory.getMemoryPoolMXBeans()
                                                                             .stream()
                                                                             .filter(bean -> "Metaspace".equalsIgnoreCase(
                                                                                 bean.getName()))
                                                                             .findFirst()
                                                                             .get();

    private static IDirectMemoryCollector directMemoryCollector;

    public static void initDirectMemoryCollector() {
        ServiceLoader<IDirectMemoryCollector> spi = ServiceLoader.load(IDirectMemoryCollector.class,
                                                                       PluginClassLoaderManager.getDefaultLoader());
        Iterator<IDirectMemoryCollector> i = spi.iterator();
        if (!i.hasNext()) {
            LoggerFactory.getLogger(MemoryMetricCollector.class)
                         .error(
                             "unable to find instance of IDirectMemoryCollector. Check if agent-plugin-jvm-jdkXXXX.jar exists.");
            System.exit(-1);
        }
        try {
            directMemoryCollector = i.next();
        } catch (UnsupportedClassVersionError e) {
            throw new AgentException("Current JRE[%s] is mismatched with the JDK that is used to compile Bithon. Make sure to use a matched JRE, or compile Bithon with a right JDK.",
                                     JmxBeans.RUNTIME_BEAN.getSpecVersion());
        }
    }

    public static MemoryCompositeMetric collectTotal() {
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
        return directMemoryCollector.collect();
    }
}
