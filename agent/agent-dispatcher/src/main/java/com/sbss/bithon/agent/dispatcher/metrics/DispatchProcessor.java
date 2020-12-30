package com.sbss.bithon.agent.dispatcher.metrics;


import com.sbss.bithon.agent.core.config.CoreConfig;
import com.sbss.bithon.agent.core.config.DispatcherConfig;
import com.sbss.bithon.agent.core.util.NetworkUtil;
import com.sbss.bithon.agent.dispatcher.WebContainer;
import com.sbss.bithon.agent.dispatcher.config.PluginSettingManager;
import com.sbss.bithon.agent.dispatcher.rpc.RpcClient;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.function.Supplier;

import static com.sbss.bithon.agent.core.Constant.QUEUE_DIR;
import static java.io.File.separator;

public class DispatchProcessor {
    private static final Logger log = LoggerFactory.getLogger(DispatchProcessor.class);

    private static final String STABLE_VERSION = "stable";
    private static DispatchProcessor instance;
    /**
     * dispatcher 状态位, 用于标识dispatcher实例的可用性, 默认 false
     */
    public boolean ready = false;
    private DispatcherConfig config;
    private FileQueue queue;
    private RpcClient sender;
    /**
     * 固化的应用端口数值, 此值在应用启动后, 不会再发生改变
     */
    private int port = 0;

    /**
     * 固化的应用名称, 此值在应用启动后, 不会再发生改变
     */
    private String appName;

    /**
     * 固化的应用ip, 此值在应用启动后, 不会再发生改变
     */
    private String ipAddress;

    /**
     * 固化的应用host, 此值在应用启动后, 不会再发生改变
     */
    private String hostName;

    private DispatchProcessor() {
    }

    public static DispatchProcessor getInstance() {
        return instance;
    }

    public static void createInstance(String agentPath) {
        if (null == instance) {
            instance = new DispatchProcessor();

            // 初始化一些启动时就可以得到的应用固有属性
            instance.appName = CoreConfig.getInstance().getBootstrap().getAppName();
            System.setProperty("appName", instance.appName);
            NetworkUtil.IpAddress ipAddress = NetworkUtil.getIpAddress();

            //支持灰度发布，将版本标识写到hostname和ip中
            String version = getVersion();

            InetAddress address =
                null != ipAddress.getInetAddress() ? ipAddress.getInetAddress() : ipAddress.getLocalInetAddress();
            instance.ipAddress = address.getHostAddress();
            instance.hostName = address.getHostName();
            if (version != null) {
                instance.ipAddress = "(" + version + ")" + instance.ipAddress;
                instance.hostName = "(" + version + ")" + instance.hostName;
            }

            //config
            instance.config = CoreConfig.getInstance().getDispatcher();

            // 创建hook方法, 延迟创建fileQueue
            Supplier<FileQueue> fileQueueInitHook = () -> {
                // 获取当前主应用的端口
                // if 端口获取成功, 创建新的fileQueue, 并将此fileQueue赋值给instance.queue, 同时将instance.ready置为true, 返回fileQueue
                if (0 != instance.port) {
                    try {
                        instance.queue =
                            new FileQueue(agentPath + separator + QUEUE_DIR + separator + CoreConfig.getInstance().getBootstrap().getAppName(), String.valueOf(instance.port));
                        instance.ready = true;
                        return instance.queue;
                    } catch (Exception e) {
                        log.error("open file queue failed! please check the agent queue path config!\n" + e);
                        // 终止全部, 防止隐含风险
                        // TODO 只终止agent, 不影响main app
                        System.exit(0);
                    }
                }
                return null;
            };

            //sender
            instance.sender = new RpcClient(instance.config);
            PluginSettingManager.init(CoreConfig.getInstance(), instance.sender);
            //collector
            new MetricsMessageSender(instance.config, fileQueueInitHook, instance.sender);

            WebContainer.addListener(new WebContainer.IContainerListener() {
                @Override
                public void onStart(WebContainer.ContainerType type,
                                    int port) {
                    log.info("web container started at {}", port);
                    instance.port = port;
                }
            });
        }
    }

    private static String getVersion() {
        String version = System.getenv("EUREKA_INSTANCE_METADATAMAP_VERSION");
        if (version == null) {
            version = System.getenv("EUREKA_INSTANCE_METADATA_MAP_VERSION");
        }
        //如果version没有设置，或者设置了stable，都理解成稳定版本，那么没必要拼接版本
        if (version == null || STABLE_VERSION.equals(version) || version.length() == 0) {
            version = null;
        }
        return version;
    }

    public void pushMessage(Object item) {
        if (log.isDebugEnabled()) {
            String className = item.getClass().getSimpleName();
            className = className.replace("Entity", "");
            log.debug("Entity : " + className + ", Got and Send : " + item.toString());
        }
        instance.queue.product(item);
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
