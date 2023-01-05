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

package org.bithon.agent.core.context;

import org.bithon.agent.core.config.AppConfiguration;
import org.bithon.agent.core.utils.NetworkUtils;
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
    private final String appName;
    private final String qualifiedAppName;
    private final String hostIp;
    private final String env;
    private final List<IAppInstanceChangedListener> listeners = Collections.synchronizedList(new ArrayList<>());
    private int port;
    private String hostAndPort;

    AppInstance(AppConfiguration appConfiguration) {
        this.appName = appConfiguration.getName();
        this.qualifiedAppName = appName + "-" + appConfiguration.getEnv();
        this.env = appConfiguration.getEnv();
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

    public String getQualifiedAppName() {
        return qualifiedAppName;
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

    public String getAppName() {
        return appName;
    }

    public String getHostIp() {
        return hostIp;
    }

    public String getHostAndPort() {
        return hostAndPort;
    }

    public String getEnv() {
        return env;
    }

    public void addListener(IAppInstanceChangedListener listener) {
        listeners.add(listener);
    }

    public interface IAppInstanceChangedListener {
        void onPortChanged(int port);
    }
}
