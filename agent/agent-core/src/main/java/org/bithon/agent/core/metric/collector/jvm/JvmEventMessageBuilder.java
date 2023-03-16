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

package org.bithon.agent.core.metric.collector.jvm;

import com.sun.management.UnixOperatingSystemMXBean;
import org.bithon.agent.AgentBuildVersion;
import org.bithon.agent.core.event.EventMessage;
import org.bithon.component.commons.time.DateTime;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author frank.chen021@outlook.com
 * @date 29/7/22 10:06 PM
 */
public class JvmEventMessageBuilder {

    public static EventMessage buildJvmStartedEventMessage() {
        Map<String, Object> args = new TreeMap<>();

        args.put("os.arch", JmxBeans.OS_BEAN.getArch());
        args.put("os.version", JmxBeans.OS_BEAN.getVersion());
        args.put("os.name", JmxBeans.OS_BEAN.getName());
        args.put("os.committedVirtualMemorySize", JmxBeans.OS_BEAN.getCommittedVirtualMemorySize());
        args.put("os.totalPhysicalMemorySize", JmxBeans.OS_BEAN.getTotalPhysicalMemorySize());
        args.put("os.totalSwapSpaceSize", JmxBeans.OS_BEAN.getTotalSwapSpaceSize());
        if (JmxBeans.OS_BEAN instanceof UnixOperatingSystemMXBean) {
            UnixOperatingSystemMXBean unixOS = (UnixOperatingSystemMXBean) JmxBeans.OS_BEAN;
            args.put("os.maxFileDescriptorCount", unixOS.getMaxFileDescriptorCount());
        }

        if (JmxBeans.RUNTIME_BEAN.isBootClassPathSupported()) {
            args.put("runtime.bootClassPath", JmxBeans.RUNTIME_BEAN.getBootClassPath().split(File.pathSeparator));
        }
        args.put("runtime.classPath", sort(Arrays.asList(JmxBeans.RUNTIME_BEAN.getClassPath().split(File.pathSeparator))));
        args.put("runtime.libraryPath", sort(Arrays.asList(JmxBeans.RUNTIME_BEAN.getLibraryPath().split(File.pathSeparator))));
        args.put("runtime.arguments", sort(new ArrayList<>(JmxBeans.RUNTIME_BEAN.getInputArguments())));

        Map<String, String> systemProperties = new TreeMap<>(JmxBeans.RUNTIME_BEAN.getSystemProperties());
        systemProperties.remove("java.class.path");
        systemProperties.remove("java.library.path");
        String bootClassPath = systemProperties.remove("sun.boot.class.path");
        if (bootClassPath != null && !args.containsKey("runtime.bootClassPath")) {
            args.put("runtime.bootClassPath", sort(Arrays.asList(bootClassPath.split(":"))));
        }
        String seperator = systemProperties.remove("line.separator");
        if (seperator != null) {
            systemProperties.put("line.separator", seperator.replaceAll("\n", "\\\\n").replaceAll("\r", "\\\\r"));
        }
        args.put("runtime.systemProperties", systemProperties);

        args.put("runtime.managementSpecVersion", JmxBeans.RUNTIME_BEAN.getManagementSpecVersion());
        args.put("runtime.name", JmxBeans.RUNTIME_BEAN.getName());
        args.put("runtime.java.name", JmxBeans.RUNTIME_BEAN.getSpecName());
        args.put("runtime.java.vendor", JmxBeans.RUNTIME_BEAN.getSpecVendor());
        args.put("runtime.java.version", JmxBeans.RUNTIME_BEAN.getSpecVersion());
        args.put("runtime.java.vm.name", JmxBeans.RUNTIME_BEAN.getVmName());
        args.put("runtime.java.vm.vendor", JmxBeans.RUNTIME_BEAN.getVmVendor());
        args.put("runtime.java.vm.version", JmxBeans.RUNTIME_BEAN.getVmVersion());
        args.put("runtime.startTime", DateTime.toISO8601(JmxBeans.RUNTIME_BEAN.getStartTime()));

        args.put("mem.heap.initial", JmxBeans.MEM_BEAN.getHeapMemoryUsage().getInit());
        args.put("mem.heap.max", JmxBeans.MEM_BEAN.getHeapMemoryUsage().getMax());

        Map<String, String> bithonProps = new TreeMap<>();
        bithonProps.put("version", AgentBuildVersion.VERSION);
        bithonProps.put("build", AgentBuildVersion.SCM_REVISION);
        bithonProps.put("timestamp", AgentBuildVersion.TIMESTAMP);
        args.put("bithon", bithonProps);

        return new EventMessage("jvm.started", args);
    }

    public static EventMessage buildStoppedEventMessage() {
        return new EventMessage("jvm.stopped", Collections.emptyMap());
    }

    private static <T extends Comparable<? super T>> List<T> sort(List<T> list) {
        Collections.sort(list);
        return list;
    }
}
