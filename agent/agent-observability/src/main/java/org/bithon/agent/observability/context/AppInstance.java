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

import org.bithon.agent.config.AppConfig;
import org.bithon.agent.configuration.ConfigurationManager;
import org.bithon.component.commons.logging.LoggerFactory;
import org.bithon.component.commons.utils.NetworkUtils;
import org.bithon.component.commons.utils.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/21 9:58 下午
 */
public class AppInstance {

    private static final AppInstance INSTANCE = new AppInstance(ConfigurationManager.getInstance().getConfig(AppConfig.class));

    private final String applicationName;
    private final String qualifiedApplicationName;
    private final String instanceIp;
    private final String env;
    private final List<IAppInstanceChangedListener> listeners = Collections.synchronizedList(new ArrayList<>());
    private int port;
    private String instanceName;
    private boolean useConfiguredName;

    private AppInstance(AppConfig appConfig) {
        this.applicationName = appConfig.getName();
        this.qualifiedApplicationName = applicationName + "-" + appConfig.getEnv();
        this.env = appConfig.getEnv();
        this.port = appConfig.getPort();

        if (StringUtils.isEmpty(appConfig.getInstance())) {
            this.instanceIp = NetworkUtils.getIpAddress().getHostAddress();
            this.instanceName = this.port > 0 ? instanceIp + ":" + this.port : instanceIp;
            this.useConfiguredName = false;
        } else {
            this.instanceIp = appConfig.getInstance();
            this.instanceName = appConfig.getInstance();
            this.useConfiguredName = true;
        }
    }

    public static AppInstance getInstance() {
        return INSTANCE;
    }

    public String getQualifiedApplicationName() {
        return qualifiedApplicationName;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
        if (!useConfiguredName) {
            this.instanceName = this.instanceIp + ":" + this.port;
        }

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

    public String getApplicationName() {
        return applicationName;
    }

    public String getInstanceIp() {
        return instanceIp;
    }

    public String getInstanceName() {
        return instanceName;
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
