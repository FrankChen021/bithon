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

package org.bithon.agent.plugin.jvm.jdk8;

import org.bithon.agent.bootstrap.expt.AgentException;
import org.bithon.agent.core.metric.domain.jvm.MemoryRegionCompositeMetric;
import org.bithon.agent.plugin.jvm.JmxBeans;
import org.bithon.agent.plugin.jvm.mem.IDirectMemoryCollector;
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

    //
    // call the VM.maxDirectMemory in static initializer so that if the VM class does not exist, a NoClassDefFoundError will be raised
    //
    static {
        try {
            VM.maxDirectMemory();
        } catch (NoClassDefFoundError e) {
            throw new AgentException(
                "JRE[%s], which is used to run this application, is mismatched with the JDK-1.8 that is used to compile Bithon. Make sure to use a matched JRE, or compile Bithon with a right JDK.",
                JmxBeans.RUNTIME_BEAN.getSpecVersion());
        }
    }

    @Override
    public MemoryRegionCompositeMetric collect() {
        long max = VM.maxDirectMemory();
        long used = DIRECT_MEMORY_BEAN.getMemoryUsed();
        return new MemoryRegionCompositeMetric(max, 0, used, max - used);
    }
}
