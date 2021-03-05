package com.sbss.bithon.agent.core.context;

import com.sbss.bithon.agent.core.utils.NetworkUtils;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/21 9:58 下午
 */
public class AppInstance {
    private static final Logger log = LoggerFactory.getLogger(AppInstance.class);

    private final String appName;
    private int port;
    private final String hostIp;
    private final String env;

    private final List<IAppInstanceChangedListener> listeners = new ArrayList<>();

    AppInstance(String appName, String env) {
        this.appName = appName + "-" + env;
        this.env = env;
        this.port = 0;

        NetworkUtils.IpAddress ipAddress = NetworkUtils.getIpAddress();
        InetAddress address = null != ipAddress.getInetAddress() ? ipAddress.getInetAddress() : ipAddress.getLocalInetAddress();
        this.hostIp = address.getHostAddress();
    }

    public String getAppName() {
        return appName;
    }

    public int getPort() {
        return port;
    }

    public String getHostIp() {
        return hostIp;
    }

    public String getEnv() {
        return env;
    }

    public void setPort(int port) {
        this.port = port;

        for (IAppInstanceChangedListener listener : listeners) {
            try {
                listener.onPortChanged(port);
            } catch (Exception e) {
                log.error("portChanged Notify failed", e);
            }
        }
    }

    public interface IAppInstanceChangedListener {
        void onPortChanged(int port);
    }

    public void addListener(IAppInstanceChangedListener listener) {
        listeners.add(listener);
    }
}
