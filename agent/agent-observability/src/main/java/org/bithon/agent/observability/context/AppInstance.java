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

package org.bithon.agent.observability.context;

import org.bithon.agent.configuration.ConfigurationManager;
import org.bithon.agent.observability.utils.NetworkUtils;
import org.bithon.component.commons.logging.LoggerFactory;
import org.bithon.component.commons.utils.StringUtils;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/21 9:58 下午
 */
public class AppInstance {

    private static final AppInstance INSTANCE = new AppInstance(ConfigurationManager.getInstance().getConfig(AppConfiguration.class));

    private final String appName;
    private final String hostIp;

    private final List<IAppInstanceChangedListener> listeners = Collections.synchronizedList(new ArrayList<>());
    private int port;
    private String hostAndPort;

    private AppInstance(AppConfiguration appConfiguration) {
        String appName = appConfiguration.getName();
        if (StringUtils.hasText(appConfiguration.getEnv())) {
            // Compatibility
            appName = appName + "-" + appConfiguration.getEnv();
        }

        this.appName = appName;
        this.port = appConfiguration.getPort();

        if (StringUtils.isEmpty(appConfiguration.getInstance())) {
            NetworkUtils.IpAddress ipAddress = NetworkUtils.getIpAddress();
            InetAddress address = null != ipAddress.getInetAddress()
                                  ? ipAddress.getInetAddress()
                                  : ipAddress.getLocalInetAddress();
            this.hostIp = address.getHostAddress();
        } else {
            this.hostIp = appConfiguration.getInstance();
        }

        this.hostAndPort = this.port > 0 ? hostIp + ":" + this.port : hostIp;
    }

    public static AppInstance getInstance() {
        return INSTANCE;
    }

    public String getAppName() {
        return appName;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
        this.hostAndPort = this.hostIp + ":" + this.port;

        // get the listeners first to avoid race condition
        IAppInstanceChangedListener[] currentListeners = listeners.toArray(new IAppInstanceChangedListener[0]);
        for (IAppInstanceChangedListener listener : currentListeners) {
            try {
                listener.onPortChanged(port);
            } catch (Exception e) {
                LoggerFactory.getLogger(AppInstance.class).error("portChanged Notify failed", e);
            }
        }
    }


    public String getHostIp() {
        return hostIp;
    }

    public String getHostAndPort() {
        return hostAndPort;
    }

    /**
     * Kept for backward compatibility
     */
    public String getEnv() {
        return "";
    }

    public void addListener(IAppInstanceChangedListener listener) {
        listeners.add(listener);
    }

    public interface IAppInstanceChangedListener {
        void onPortChanged(int port);
    }
}
