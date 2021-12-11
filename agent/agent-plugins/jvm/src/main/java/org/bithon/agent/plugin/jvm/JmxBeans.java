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

package org.bithon.agent.plugin.jvm;

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
