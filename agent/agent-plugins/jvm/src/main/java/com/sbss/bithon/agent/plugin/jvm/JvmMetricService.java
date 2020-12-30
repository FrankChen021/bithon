package com.sbss.bithon.agent.plugin.jvm;

import com.sbss.bithon.agent.core.context.AgentContext;
import com.sbss.bithon.agent.core.dispatcher.Dispatcher;
import com.sbss.bithon.agent.core.dispatcher.Dispatchers;
import com.sbss.bithon.agent.core.dispatcher.IMessageConverter;
import com.sbss.bithon.agent.core.events.EventMessage;
import com.sbss.bithon.agent.core.metrics.jvm.JvmMetrics;
import com.sbss.bithon.agent.core.utils.time.DateTime;
import com.sun.management.UnixOperatingSystemMXBean;
import shaded.com.alibaba.fastjson.JSON;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;

import static com.sbss.bithon.agent.plugin.jvm.JmxBeans.osBean;
import static com.sbss.bithon.agent.plugin.jvm.JmxBeans.runtimeBean;

/**
 * @author frankchen
 */
public class JvmMetricService {
    private static final Logger log = LoggerFactory.getLogger(JvmMetricService.class);

    private Dispatcher metricsDispatcher;

    private boolean jvmStarted = false;

    private CpuMetricsBuilder cpuMetricsBuilder;
    private GcMetricsBuilder gcMetricsBuilder;

    public void start() {
        int checkIntervalSeconds = 10;

        metricsDispatcher = Dispatchers.getOrCreate(Dispatchers.DISPATCHER_NAME_METRICS);
        gcMetricsBuilder = new GcMetricsBuilder();
        cpuMetricsBuilder = new CpuMetricsBuilder();

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
            sendJvmMetrics();
            }
        }, checkIntervalSeconds * 1000, checkIntervalSeconds * 1000);
    }

    private void sendJvmMetrics() {
        if (!metricsDispatcher.isReady()) {
            return;
        }

        try {
            IMessageConverter converter = metricsDispatcher.getMessageConverter();
            metricsDispatcher.sendMessage(converter.from(AgentContext.getInstance().getAppInstance(),
                                                         System.currentTimeMillis(),
                                                         10,
                                                         buildJvmMetrics()));
            if (!jvmStarted) {
                sendJvmStartedEvent();
                jvmStarted = true;
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private JvmMetrics buildJvmMetrics() {
        JvmMetrics jvmMetrics = new JvmMetrics(runtimeBean.getUptime(),
                                               runtimeBean.getStartTime());
        jvmMetrics.cpuMetrics = cpuMetricsBuilder.build();
        jvmMetrics.memoryMetrics = MemoryMetricsBuilder.buildMemoryMetrics();
        jvmMetrics.heapMetrics = MemoryMetricsBuilder.buildHeapMetrics();
        jvmMetrics.nonHeapMetrics = MemoryMetricsBuilder.buildNonHeapMetrics();
        jvmMetrics.metaspaceMetrics = MemoryMetricsBuilder.buildMetaspaceMetrics();
        jvmMetrics.gcMetrics = gcMetricsBuilder.build();
        jvmMetrics.threadMetrics = ThreadMetricsBuilder.build();
        jvmMetrics.classMetrics = ClassMetricsBuilder.build();
        return jvmMetrics;
    }


    private void sendJvmStartedEvent() {
        Dispatcher dispatcher = Dispatchers.getOrCreate(Dispatchers.DISPATCHER_NAME_EVNETS);
        IMessageConverter converter = dispatcher.getMessageConverter();
        dispatcher.sendMessage(converter.from(AgentContext.getInstance().getAppInstance(),
                                              buildJvmStartedEventMessage()));
    }

    private EventMessage buildJvmStartedEventMessage() {
        Map<String, String> args = new TreeMap<>();

        args.put("os.arch", osBean.getArch());
        args.put("os.version", osBean.getVersion());
        args.put("os.name", osBean.getName());
        args.put("os.committedVirtualMemorySize", String.valueOf(osBean.getCommittedVirtualMemorySize()));
        args.put("os.totalPhysicalMemorySize", String.valueOf(osBean.getTotalPhysicalMemorySize()));
        args.put("os.totalSwapSpaceSize", String.valueOf(osBean.getTotalSwapSpaceSize()));
        if (osBean instanceof UnixOperatingSystemMXBean) {
            UnixOperatingSystemMXBean unixOperatingSystemMXBean = (UnixOperatingSystemMXBean) osBean;
            args.put("os.maxFileDescriptorCount", String.valueOf(unixOperatingSystemMXBean.getMaxFileDescriptorCount()));
        }

        //TODO: remove JSON dependency by manual serialization
        args.put("runtime.bootClassPath", JSON.toJSONString(runtimeBean.getBootClassPath().split(File.pathSeparator)));
        args.put("runtime.classPath", JSON.toJSONString(runtimeBean.getClassPath().split(File.pathSeparator)));
        args.put("runtime.arguments", JSON.toJSONString(runtimeBean.getInputArguments()));
        args.put("runtime.libraryPath", JSON.toJSONString(runtimeBean.getLibraryPath().split(File.pathSeparator)));
        args.put("runtime.systemProperties", JSON.toJSONString(runtimeBean.getSystemProperties()));
        args.put("runtime.managementSpecVersion", runtimeBean.getManagementSpecVersion());
        args.put("runtime.name", runtimeBean.getName());
        args.put("runtime.java.name", runtimeBean.getSpecName());
        args.put("runtime.java.vendor", runtimeBean.getSpecVendor());
        args.put("runtime.java.version", runtimeBean.getSpecVersion());
        args.put("runtime.java.vm.name", runtimeBean.getVmName());
        args.put("runtime.java.vm.vendor", runtimeBean.getVmVendor());
        args.put("runtime.java.vm.version", runtimeBean.getVmVersion());
        args.put("runtime.startTime", DateTime.toISO8601(runtimeBean.getStartTime()));

        args.put("mem.heap.initial", String.valueOf(JmxBeans.memoryBean.getHeapMemoryUsage().getInit()));
        args.put("mem.heap.max", String.valueOf(JmxBeans.memoryBean.getHeapMemoryUsage().getMax()));

        return new EventMessage("jvm.started", args);
    }
}
