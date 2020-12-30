package com.sbss.bithon.agent.dispatcher.ktrace;

import com.keruyun.commons.agent.collector.entity.TraceLogEntity;
import com.sbss.bithon.agent.core.config.CoreConfig;
import com.sbss.bithon.agent.core.config.DispatcherConfig;
import com.sbss.bithon.agent.core.util.NetworkUtil;
import com.sbss.bithon.agent.dispatcher.WebContainer;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.function.Supplier;

import static com.sbss.bithon.agent.core.Constant.TRACE_QUEUE_DIR;
import static java.io.File.separator;

public class DispatchTraceProcessor {
    private static final Logger log = LoggerFactory.getLogger(DispatchTraceProcessor.class);

    private static DispatchTraceProcessor instance;
    /**
     * dispatcher 状态位, 用于标识dispatcher实例的可用性, 默认 false
     */
    public boolean ready = false;
    DispatcherConfig config;
    /**
     * 固化的应用端口数值, 此值在应用启动后, 不会再发生改变
     */
    int port = 0;
    /**
     * 固化的应用名称, 此值在应用启动后, 不会再发生改变
     */
    String appName;
    /**
     * 固化的应用ip, 此值在应用启动后, 不会再发生改变
     */
    String ipAddress;
    /**
     * 固化的应用host, 此值在应用启动后, 不会再发生改变
     */
    String hostName;
    private boolean started = false;
    private FileTraceQueue queue;
    private String agentLocation;

    private DispatchTraceProcessor() {
    }

    public static DispatchTraceProcessor getInstance() {
        return instance;
    }

    public static void createInstance(String agentPath) {
        if (null == instance) {

            instance = new DispatchTraceProcessor();

            instance.agentLocation = agentPath;

            // 初始化一些启动时就可以得到的应用固有属性
            instance.appName = CoreConfig.getInstance().getBootstrap().getAppName();
            System.setProperty("appName", instance.appName);
            NetworkUtil.IpAddress ipAddress = NetworkUtil.getIpAddress();
            InetAddress address = null != ipAddress.getInetAddress() ? ipAddress.getInetAddress()
                : ipAddress.getLocalInetAddress();
            instance.ipAddress = address.getHostAddress();
            instance.hostName = address.getHostName();
            // config
            instance.config = CoreConfig.getInstance().getDispatcher();

            WebContainer.addListener((type, port) -> {
                log.info("container started at {}", port);
                instance.port = port;
            });
        }
    }

    public void pushMessage(TraceLogEntity trace) {
        if (!ready) {
            return;
        }

        queue.product(trace);
    }

    /**
     * @return true - 首次启动
     */
    public boolean start() {
        if (started) {
            return false;
        }

        synchronized (this) {
            if (started) { //double check
                return false;
            }
            log.info(Banner.TEXT);
            started = true; //先不管启动成功与否

            // 创建hook方法, 延迟创建fileQueue
            Supplier<FileTraceQueue> fileQueueInitHook = () -> {
                // 获取当前主应用的端口
                // if 端口获取成功, 创建新的fileQueue, 并将此fileQueue赋值给instance.queue,
                // 同时将instance.ready置为true, 返回fileQueue
                if (0 != port) {
                    try {
                        queue = new FileTraceQueue(agentLocation + separator + TRACE_QUEUE_DIR + separator +
                                                       instance.appName,
                                                   String.valueOf(instance.port));
                        ready = true;
                        return queue;
                    } catch (Exception e) {
                        log.error("open file queue failed! please check the agent queue path config!\n" + e);
                    }
                }
                return null;
            };

            // collector
            new TraceMessageSender(this, fileQueueInitHook);
        }

        return true;
    }

    public int getPort() {
        return this.port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getAppName() {
        return appName;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getHostName() {
        return hostName;
    }
}
