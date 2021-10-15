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
import org.bithon.agent.core.dispatcher.Dispatcher;
import org.bithon.agent.core.dispatcher.Dispatchers;
import org.bithon.agent.core.dispatcher.IMessageConverter;
import org.bithon.agent.core.event.EventMessage;
import org.bithon.agent.core.metric.collector.IMetricCollector;
import org.bithon.agent.core.metric.collector.MetricCollectorManager;
import org.bithon.agent.core.metric.domain.jvm.GcCompositeMetric;
import org.bithon.agent.core.metric.domain.jvm.JvmMetricSet;
import org.bithon.agent.core.utils.time.DateTime;
import org.bithon.agent.plugin.jvm.gc.GcMetricCollector;
import org.bithon.agent.plugin.jvm.mem.ClassMetricCollector;
import org.bithon.agent.plugin.jvm.mem.MemoryMetricCollector;
import shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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
                for (GcCompositeMetric gcMetricSet : gcCollector.collect()) {
                    metricMessages.add(messageConverter.from(timestamp,
                                                             interval,
                                                             gcMetricSet));
                }
                return metricMessages;
            }
        });
    }

    private JvmMetricSet buildJvmMetrics() {
        JvmMetricSet jvmMetricSet = new JvmMetricSet(JmxBeans.RUNTIME_BEAN.getUptime(),
                                                     JmxBeans.RUNTIME_BEAN.getStartTime());
        jvmMetricSet.cpuMetricSet = cpuMetricCollector.collect();
        jvmMetricSet.memoryMetricSet = MemoryMetricCollector.collectTotal();
        jvmMetricSet.heapMetricSet = MemoryMetricCollector.collectHeap();
        jvmMetricSet.nonHeapMetricSet = MemoryMetricCollector.collectNonHeap();
        jvmMetricSet.metaspaceMetricSet = MemoryMetricCollector.collectMetaSpace();
        jvmMetricSet.directMemMetricSet = MemoryMetricCollector.collectDirectMemory();
        jvmMetricSet.threadMetricSet = ThreadMetricCollector.collect();
        jvmMetricSet.classMetricSet = ClassMetricCollector.collect();
        return jvmMetricSet;
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
        Map<String, String> args = new TreeMap<>();

        args.put("os.arch", JmxBeans.OS_BEAN.getArch());
        args.put("os.version", JmxBeans.OS_BEAN.getVersion());
        args.put("os.name", JmxBeans.OS_BEAN.getName());
        args.put("os.committedVirtualMemorySize", String.valueOf(JmxBeans.OS_BEAN.getCommittedVirtualMemorySize()));
        args.put("os.totalPhysicalMemorySize", String.valueOf(JmxBeans.OS_BEAN.getTotalPhysicalMemorySize()));
        args.put("os.totalSwapSpaceSize", String.valueOf(JmxBeans.OS_BEAN.getTotalSwapSpaceSize()));
        if (JmxBeans.OS_BEAN instanceof UnixOperatingSystemMXBean) {
            UnixOperatingSystemMXBean unixOperatingSystemMXBean = (UnixOperatingSystemMXBean) JmxBeans.OS_BEAN;
            args.put("os.maxFileDescriptorCount",
                     String.valueOf(unixOperatingSystemMXBean.getMaxFileDescriptorCount()));
        }

        ObjectMapper om = new ObjectMapper();
        try {
            if (JmxBeans.RUNTIME_BEAN.isBootClassPathSupported()) {
                args.put("runtime.bootClassPath",
                         om.writeValueAsString(JmxBeans.RUNTIME_BEAN.getBootClassPath().split(File.pathSeparator)));
            }
            args.put("runtime.classPath", om.writeValueAsString(JmxBeans.RUNTIME_BEAN.getClassPath().split(File.pathSeparator)));
            args.put("runtime.arguments", om.writeValueAsString(JmxBeans.RUNTIME_BEAN.getInputArguments()));
            args.put("runtime.libraryPath",
                     om.writeValueAsString(JmxBeans.RUNTIME_BEAN.getLibraryPath().split(File.pathSeparator)));
            args.put("runtime.systemProperties", om.writeValueAsString(JmxBeans.RUNTIME_BEAN.getSystemProperties()));
        } catch (IOException ignored) {
        }
        args.put("runtime.managementSpecVersion", JmxBeans.RUNTIME_BEAN.getManagementSpecVersion());
        args.put("runtime.name", JmxBeans.RUNTIME_BEAN.getName());
        args.put("runtime.java.name", JmxBeans.RUNTIME_BEAN.getSpecName());
        args.put("runtime.java.vendor", JmxBeans.RUNTIME_BEAN.getSpecVendor());
        args.put("runtime.java.version", JmxBeans.RUNTIME_BEAN.getSpecVersion());
        args.put("runtime.java.vm.name", JmxBeans.RUNTIME_BEAN.getVmName());
        args.put("runtime.java.vm.vendor", JmxBeans.RUNTIME_BEAN.getVmVendor());
        args.put("runtime.java.vm.version", JmxBeans.RUNTIME_BEAN.getVmVersion());
        args.put("runtime.startTime", DateTime.toISO8601(JmxBeans.RUNTIME_BEAN.getStartTime()));

        args.put("mem.heap.initial", String.valueOf(JmxBeans.MEM_BEAN.getHeapMemoryUsage().getInit()));
        args.put("mem.heap.max", String.valueOf(JmxBeans.MEM_BEAN.getHeapMemoryUsage().getMax()));

        return new EventMessage("jvm.started", args);
    }
}
