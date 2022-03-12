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

import com.sun.management.UnixOperatingSystemMXBean;
import org.bithon.agent.AgentBuildVersion;
import org.bithon.agent.core.dispatcher.Dispatcher;
import org.bithon.agent.core.dispatcher.Dispatchers;
import org.bithon.agent.core.dispatcher.IMessageConverter;
import org.bithon.agent.core.event.EventMessage;
import org.bithon.agent.core.metric.collector.IMetricCollector;
import org.bithon.agent.core.metric.collector.MetricCollectorManager;
import org.bithon.agent.core.metric.domain.jvm.GcMetrics;
import org.bithon.agent.core.metric.domain.jvm.JvmMetrics;
import org.bithon.component.commons.time.DateTime;
import org.bithon.agent.plugin.jvm.gc.GcMetricCollector;
import org.bithon.agent.plugin.jvm.mem.ClassMetricCollector;
import org.bithon.agent.plugin.jvm.mem.MemoryMetricCollector;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;

/**
 * A facade collector which collects jvm related metrics from other collectors
 *
 * @author frankchen
 */
public class JvmMetricCollector {

    private CpuMetricCollector cpuMetricCollector;


    public void start() {
        MemoryMetricCollector.initDirectMemoryCollector();

        cpuMetricCollector = new CpuMetricCollector();

        //
        // start timer to send event
        //
        Timer sendEventTimer = new Timer("bithon-event-sender");
        sendEventTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (sendJvmStartedEvent()) {
                    sendEventTimer.cancel();
                }
            }
        }, 1000, 5);

        //
        // register collector
        //
        MetricCollectorManager.getInstance().register("jvm-metrics", new IMetricCollector() {
            @Override
            public boolean isEmpty() {
                return false;
            }

            @Override
            public List<Object> collect(IMessageConverter messageConverter,
                                        int interval,
                                        long timestamp) {
                return Collections.singletonList(messageConverter.from(timestamp,
                                                                       interval,
                                                                       buildJvmMetrics()));
            }
        });

        MetricCollectorManager.getInstance().register("jvm-gc-metrics", new IMetricCollector() {
            private final GcMetricCollector gcCollector = new GcMetricCollector();

            @Override
            public boolean isEmpty() {
                return false;
            }

            @Override
            public List<Object> collect(IMessageConverter messageConverter, int interval, long timestamp) {
                List<Object> metricMessages = new ArrayList<>(2);
                for (GcMetrics gcMetricSet : gcCollector.collect()) {
                    metricMessages.add(messageConverter.from(timestamp,
                                                             interval,
                                                             gcMetricSet));
                }
                return metricMessages;
            }
        });
    }

    private JvmMetrics buildJvmMetrics() {
        JvmMetrics jvmMetrics = new JvmMetrics(JmxBeans.RUNTIME_BEAN.getUptime(),
                                               JmxBeans.RUNTIME_BEAN.getStartTime());
        jvmMetrics.cpu = cpuMetricCollector.collect();
        jvmMetrics.memory = MemoryMetricCollector.collectTotal();
        jvmMetrics.heap = MemoryMetricCollector.collectHeap();
        jvmMetrics.nonHeap = MemoryMetricCollector.collectNonHeap();
        jvmMetrics.metaspace = MemoryMetricCollector.collectMetaSpace();
        jvmMetrics.directMemory = MemoryMetricCollector.collectDirectMemory();
        jvmMetrics.thread = ThreadMetricCollector.collect();
        jvmMetrics.clazz = ClassMetricCollector.collect();
        return jvmMetrics;
    }

    private boolean sendJvmStartedEvent() {
        Dispatcher dispatcher = Dispatchers.getOrCreate(Dispatchers.DISPATCHER_NAME_EVENT);
        if (!dispatcher.isReady()) {
            return false;
        }

        IMessageConverter converter = dispatcher.getMessageConverter();
        dispatcher.sendMessage(converter.from(buildJvmStartedEventMessage()));
        return true;
    }

    private EventMessage buildJvmStartedEventMessage() {
        Map<String, Object> args = new TreeMap<>();

        args.put("os.arch", JmxBeans.OS_BEAN.getArch());
        args.put("os.version", JmxBeans.OS_BEAN.getVersion());
        args.put("os.name", JmxBeans.OS_BEAN.getName());
        args.put("os.committedVirtualMemorySize", JmxBeans.OS_BEAN.getCommittedVirtualMemorySize());
        args.put("os.totalPhysicalMemorySize", JmxBeans.OS_BEAN.getTotalPhysicalMemorySize());
        args.put("os.totalSwapSpaceSize", JmxBeans.OS_BEAN.getTotalSwapSpaceSize());
        if (JmxBeans.OS_BEAN instanceof UnixOperatingSystemMXBean) {
            UnixOperatingSystemMXBean unixOperatingSystemMXBean = (UnixOperatingSystemMXBean) JmxBeans.OS_BEAN;
            args.put("os.maxFileDescriptorCount", unixOperatingSystemMXBean.getMaxFileDescriptorCount());
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

    private <T extends Comparable<? super T>> List<T> sort(List<T> list) {
        Collections.sort(list);
        return list;
    }
}
