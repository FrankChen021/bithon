/*
 *    Copyright 2020 bithon.cn
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

import org.bithon.agent.core.utils.NetworkUtils;
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

    private final String rawAppName;
    private final String appName;
    private final String hostIp;
    private final String env;
    private final List<IAppInstanceChangedListener> listeners = new ArrayList<>();
    private int port;

    AppInstance(String appName, String env) {
        this.rawAppName = appName;
        this.appName = appName + "-" + env;
        this.env = env;
        this.port = 0;

        NetworkUtils.IpAddress ipAddress = NetworkUtils.getIpAddress();
        InetAddress address = null != ipAddress.getInetAddress()
                              ? ipAddress.getInetAddress()
                              : ipAddress.getLocalInetAddress();
        this.hostIp = address.getHostAddress();
    }

    public String getAppName() {
        return appName;
    }

    public int getPort() {
        return port;
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

    public String getRawAppName() {
        return rawAppName;
    }

    public String getHostIp() {
        return hostIp;
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
