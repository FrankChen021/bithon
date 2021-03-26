package com.sbss.bithon.agent.plugin.jvm;

import com.sbss.bithon.agent.core.dispatcher.Dispatcher;
import com.sbss.bithon.agent.core.dispatcher.Dispatchers;
import com.sbss.bithon.agent.core.dispatcher.IMessageConverter;
import com.sbss.bithon.agent.core.event.EventMessage;
import com.sbss.bithon.agent.core.metric.collector.IMetricCollector;
import com.sbss.bithon.agent.core.metric.collector.MetricCollectorManager;
import com.sbss.bithon.agent.core.metric.domain.jvm.JvmMetricSet;
import com.sbss.bithon.agent.core.utils.time.DateTime;
import com.sun.management.UnixOperatingSystemMXBean;
import shaded.com.alibaba.fastjson.JSON;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;

import static com.sbss.bithon.agent.plugin.jvm.JmxBeans.OS_BEAN;
import static com.sbss.bithon.agent.plugin.jvm.JmxBeans.RUNTIME_BEAN;

/**
 * A facade collector which collects jvm related metrics from other collectors
 *
 * @author frankchen
 */
public class JvmMetricCollector {

    private CpuMetricCollector cpuMetricCollector;
    private GcMetricCollector gcMetricCollector;

    public void start() {
        gcMetricCollector = new GcMetricCollector();
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
    }

    private JvmMetricSet buildJvmMetrics() {
        JvmMetricSet jvmMetricSet = new JvmMetricSet(RUNTIME_BEAN.getUptime(),
                                                     RUNTIME_BEAN.getStartTime());
        jvmMetricSet.cpuMetricsSet = cpuMetricCollector.collect();
        jvmMetricSet.memoryMetricsSet = MemoryMetricCollector.buildMemoryMetrics();
        jvmMetricSet.heapMetricsSet = MemoryMetricCollector.collectHeap();
        jvmMetricSet.nonHeapMetricsSet = MemoryMetricCollector.collectNonHeap();
        jvmMetricSet.metaspaceMetricsSet = MemoryMetricCollector.collectMeataSpace();
        jvmMetricSet.gcCompositeMetrics = gcMetricCollector.collect();
        jvmMetricSet.threadMetricsSet = ThreadMetricCollector.collect();
        jvmMetricSet.classMetricsSet = ClassMetricCollector.collect();
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

        args.put("os.arch", OS_BEAN.getArch());
        args.put("os.version", OS_BEAN.getVersion());
        args.put("os.name", OS_BEAN.getName());
        args.put("os.committedVirtualMemorySize", String.valueOf(OS_BEAN.getCommittedVirtualMemorySize()));
        args.put("os.totalPhysicalMemorySize", String.valueOf(OS_BEAN.getTotalPhysicalMemorySize()));
        args.put("os.totalSwapSpaceSize", String.valueOf(OS_BEAN.getTotalSwapSpaceSize()));
        if (OS_BEAN instanceof UnixOperatingSystemMXBean) {
            UnixOperatingSystemMXBean unixOperatingSystemMXBean = (UnixOperatingSystemMXBean) OS_BEAN;
            args.put("os.maxFileDescriptorCount",
                     String.valueOf(unixOperatingSystemMXBean.getMaxFileDescriptorCount()));
        }

        //TODO: remove JSON dependency by manual serialization
        args.put("runtime.bootClassPath", JSON.toJSONString(RUNTIME_BEAN.getBootClassPath().split(File.pathSeparator)));
        args.put("runtime.classPath", JSON.toJSONString(RUNTIME_BEAN.getClassPath().split(File.pathSeparator)));
        args.put("runtime.arguments", JSON.toJSONString(RUNTIME_BEAN.getInputArguments()));
        args.put("runtime.libraryPath", JSON.toJSONString(RUNTIME_BEAN.getLibraryPath().split(File.pathSeparator)));
        args.put("runtime.systemProperties", JSON.toJSONString(RUNTIME_BEAN.getSystemProperties()));
        args.put("runtime.managementSpecVersion", RUNTIME_BEAN.getManagementSpecVersion());
        args.put("runtime.name", RUNTIME_BEAN.getName());
        args.put("runtime.java.name", RUNTIME_BEAN.getSpecName());
        args.put("runtime.java.vendor", RUNTIME_BEAN.getSpecVendor());
        args.put("runtime.java.version", RUNTIME_BEAN.getSpecVersion());
        args.put("runtime.java.vm.name", RUNTIME_BEAN.getVmName());
        args.put("runtime.java.vm.vendor", RUNTIME_BEAN.getVmVendor());
        args.put("runtime.java.vm.version", RUNTIME_BEAN.getVmVersion());
        args.put("runtime.startTime", DateTime.toISO8601(RUNTIME_BEAN.getStartTime()));

        args.put("mem.heap.initial", String.valueOf(JmxBeans.MEM_BEAN.getHeapMemoryUsage().getInit()));
        args.put("mem.heap.max", String.valueOf(JmxBeans.MEM_BEAN.getHeapMemoryUsage().getMax()));

        return new EventMessage("jvm.started", args);
    }
}
