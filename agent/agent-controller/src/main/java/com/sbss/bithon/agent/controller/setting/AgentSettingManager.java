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

package com.sbss.bithon.agent.controller.setting;


import com.sbss.bithon.agent.controller.IAgentController;
import com.sbss.bithon.agent.core.utils.CollectionUtils;
import shaded.com.fasterxml.jackson.databind.DeserializationFeature;
import shaded.com.fasterxml.jackson.databind.JsonNode;
import shaded.com.fasterxml.jackson.databind.ObjectMapper;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

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
    private final IAgentController controller;
    private final Map<String, List<IAgentSettingRefreshListener>> listeners;
    private Long lastModifiedAt = 0L;

    public static void createInstance(String appName, String env, IAgentController controller) {
        INSTANCE = new AgentSettingManager(appName, env, controller);
        INSTANCE.start();
    }

    public static AgentSettingManager getInstance() {
        return INSTANCE;
    }

    private AgentSettingManager(String appName, String env, IAgentController controller) {
        this.appName = appName;
        this.env = env;
        this.controller = controller;
        this.listeners = new ConcurrentHashMap<>();
    }

    public void register(SettingRootNames name, IAgentSettingRefreshListener listener) {
        listeners.computeIfAbsent(name.getName(), key -> new ArrayList<>()).add(listener);
    }

    private void start() {
        if (controller != null) {
            new Timer("setting-fetcher").schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        fetchSettings();
                    } catch (Throwable e) {
                        log.error("Failed to fetch plugin settings", e);
                    }
                }
            }, 3000, 60 * 1000);
        }
    }

    private void fetchSettings() {
        log.info("Fetch setting for {}-{}", appName, env);

        Map<String, String> settings = controller.fetch(appName, env, lastModifiedAt);
        if (CollectionUtils.isEmpty(settings)) {
            return;
        }

        ObjectMapper om = new ObjectMapper();
        om.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        settings.forEach((sectionName, settingString) -> {
            List<IAgentSettingRefreshListener> listeners = this.listeners.get(sectionName);
            if (CollectionUtils.isEmpty(listeners)) {
                return;
            }

            JsonNode configNode;
            try {
                configNode = om.readTree(settingString);
            } catch (Exception e) {
                log.warn("Can't deserialize setting for {}.\n{}", sectionName, settingString);
                return;
            }

            for (IAgentSettingRefreshListener listener : listeners) {
                try {
                    listener.onRefresh(om, configNode);
                } catch (Exception e) {
                    log.warn(String.format("Exception when refresh setting %s.\n%s", sectionName, settingString), e);
                }
            }
        });
        lastModifiedAt = System.currentTimeMillis();
    }
}
