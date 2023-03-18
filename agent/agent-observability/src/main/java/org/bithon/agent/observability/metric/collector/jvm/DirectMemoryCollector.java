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

import org.bithon.agent.bootstrap.expt.AgentException;
import org.bithon.agent.observability.metric.domain.jvm.MemoryRegionMetrics;

import java.lang.management.BufferPoolMXBean;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

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

    private long max = 0;

    //
    // call the VM.maxDirectMemory in static initializer so that if the VM class does not exist, a NoClassDefFoundError will be raised
    //
    DirectMemoryCollector() throws AgentException {
        String[][] providers = new String[][]{
            {"sun.misc.VM", "maxDirectMemory"},
            {"jdk.internal.misc.VM", "maxDirectMemory"}
        };

        Method maxMaxMethod = null;
        for (String[] provider : providers) {
            String clazz = provider[0];
            String method = provider[1];
            try {
                Class<?> vmClass = Class.forName(clazz);
                maxMaxMethod = vmClass.getDeclaredMethod(method);

                // even though the method is public
                // we call this method to trigger the InaccessibleObjectException if required argument is missing
                maxMaxMethod.setAccessible(true);
                max = (long) maxMaxMethod.invoke(null);
                break;
            } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
            } catch (Exception e) {
                if ("java.lang.reflect.InaccessibleObjectException".equals(e.getClass().getName())) {
                    throw new AgentException(
                        "Bithon requires the access to VM.maxDirectMemory() to monitor the direct memory. For applications running under JRE[%s], please add this argument(--add-exports java.base/jdk.internal.misc=ALL-UNNAMED) to your application command line to grant access.",
                        JmxBeans.RUNTIME_BEAN.getSpecVersion());
                }
            }
        }

        if (maxMaxMethod == null) {
            throw new AgentException(
                "The application is running under JRE[%s]. But the VM class is not found.",
                JmxBeans.RUNTIME_BEAN.getSpecVersion());
        }
    }

    public MemoryRegionMetrics collect() {
        long used = DIRECT_MEMORY_BEAN.getMemoryUsed();
        return new MemoryRegionMetrics(max, 0, used, max - used);
    }
}
