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
import org.bithon.agent.core.metric.domain.jvm.MemoryMetrics;
import org.bithon.agent.core.metric.domain.jvm.MemoryRegionMetrics;
import org.bithon.agent.core.plugin.PluginClassLoaderManager;
import org.bithon.agent.plugin.jvm.JmxBeans;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.util.Iterator;
import java.util.ServiceConfigurationError;
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
            throw new AgentException("Unable to find instance of IDirectMemoryCollector. Check if agent-plugin-jvm-jdkXXXX.jar exists.");
        }
        try {
            directMemoryCollector = i.next();
        } catch (UnsupportedClassVersionError e) {
            throw new AgentException(
                "JRE[%s], which is used to run this application, is mismatched with the JDK that is used to compile Bithon. Make sure to use a matched JRE, or compile Bithon with a right JDK.",
                JmxBeans.RUNTIME_BEAN.getSpecVersion());
        } catch (ServiceConfigurationError e) {
            Throwable cause = e.getCause();
            while (cause.getCause() != null) {
                cause = cause.getCause();
            }
            if (cause instanceof AgentException) {
                throw (AgentException) cause;
            } else {
                throw new AgentException(cause);
            }
        }
    }

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
        return directMemoryCollector.collect();
    }
}
