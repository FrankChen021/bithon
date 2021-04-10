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

package com.sbss.bithon.agent.core.setting;


import com.sbss.bithon.agent.core.config.FetcherConfig;
import com.sbss.bithon.agent.core.context.AppInstance;
import com.sbss.bithon.agent.core.utils.CollectionUtils;
import com.sbss.bithon.agent.core.utils.StringUtils;
import shaded.com.alibaba.fastjson.JSONObject;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Dynamic Setting Manager for Plugins
 *
 * @author frank.chen021@outlook.com
 */
public class AgentSettingManager {
    private static final Logger log = LoggerFactory.getLogger(AgentSettingManager.class);

    private static AgentSettingManager INSTANCE = null;

    private final String appName;
    private final String env;
    private final IAgentSettingFetcher settingFetcher;
    private final Map<String, List<IAgentSettingRefreshListener>> listeners;
    private Long lastModifiedAt = 0L;

    public AgentSettingManager(String appName, String env, IAgentSettingFetcher settingFetcher) {
        this.appName = appName;
        this.env = env;
        this.settingFetcher = settingFetcher;
        this.listeners = new HashMap<>();
    }

    public static synchronized void createInstance(AppInstance appInstance,
                                                   FetcherConfig fetcherConfig) throws Exception {
        if (INSTANCE != null) {
            return;
        }
        IAgentSettingFetcher fetcher = null;
        if (fetcherConfig != null && !StringUtils.isEmpty(fetcherConfig.getClient())) {
            try {
                IAgentSettingFetcherFactory factory = (IAgentSettingFetcherFactory) Class.forName(fetcherConfig.getClient())
                                                                                         .newInstance();
                fetcher = factory.createFetcher(fetcherConfig);
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                log.error("Can't create instanceof fetcher {}", fetcherConfig.getClient());
                throw e;
            }
        } else {
            log.warn("Fetcher Impl has not configured.");
        }
        INSTANCE = new AgentSettingManager(appInstance.getAppName(),
                                           appInstance.getEnv(),
                                           fetcher);
        INSTANCE.start();
    }

    public static AgentSettingManager getInstance() {
        return INSTANCE;
    }

    public void register(SettingRootNames name, IAgentSettingRefreshListener listener) {
        listeners.computeIfAbsent(name.getName(), key -> new ArrayList<>()).add(listener);
    }

    private void start() {
        if (settingFetcher != null) {
            new Timer("setting-fetcher").schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        fetchSettings();
                        lastModifiedAt = System.currentTimeMillis();
                    } catch (Exception e) {
                        log.error("Failed to fetch plugin settings", e);
                    }
                }
            }, 3000, 60 * 1000);
        }
    }

    private void fetchSettings() {
        Map<String, String> settings = settingFetcher.fetch(appName, env, lastModifiedAt);
        if (CollectionUtils.isEmpty(settings)) {
            return;
        }

        settings.forEach((sectionName, settingString) -> {
            List<IAgentSettingRefreshListener> listeners = this.listeners.get(sectionName);
            if (CollectionUtils.isEmpty(listeners)) {
                return;
            }

            JSONObject sectionSetting;
            try {
                sectionSetting = JSONObject.parseObject(settingString);
            } catch (Exception e) {
                log.warn("Can't deserialize plugin setting for {}.\n{}", sectionName, settingString);
                return;
            }

            // DON'T catch exception so that there's a chance to retry on next round
            for (IAgentSettingRefreshListener listener : listeners) {
                listener.onRefresh(sectionSetting);
            }
        });
    }
}
