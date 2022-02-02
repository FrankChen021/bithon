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

package org.bithon.agent.controller.setting;


import org.bithon.agent.controller.IAgentController;
import org.bithon.component.commons.utils.CollectionUtils;
import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.LoggerFactory;
import shaded.com.fasterxml.jackson.databind.DeserializationFeature;
import shaded.com.fasterxml.jackson.databind.JsonNode;
import shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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
    private static final ILogAdaptor log = LoggerFactory.getLogger(AgentSettingManager.class);

    private static AgentSettingManager INSTANCE = null;

    private final String appName;
    private final String env;
    private final IAgentController controller;
    private final Map<String, List<IAgentSettingRefreshListener>> listeners;
    private Long lastModifiedAt = 0L;
    private Map<String, JsonNode> latestSettings = Collections.emptyMap();
    private ObjectMapper om;

    private AgentSettingManager(String appName, String env, IAgentController controller) {
        this.appName = appName;
        this.env = env;
        this.controller = controller;
        this.listeners = new ConcurrentHashMap<>();
        this.om = new ObjectMapper();
        om.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public static void createInstance(String appName, String env, IAgentController controller) {
        INSTANCE = new AgentSettingManager(appName, env, controller);
        INSTANCE.start();
    }

    public static AgentSettingManager getInstance() {
        return INSTANCE;
    }

    public Map<String, JsonNode> getLatestSettings() {
        return latestSettings;
    }

    public void register(String name, IAgentSettingRefreshListener listener) {
        listeners.computeIfAbsent(name, key -> new ArrayList<>()).add(listener);
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

        Map<String, JsonNode> latestSettings = new HashMap<>();
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

            latestSettings.put(sectionName, configNode);

            for (IAgentSettingRefreshListener listener : listeners) {
                try {
                    listener.onRefresh(om, configNode);
                } catch (Exception e) {
                    log.warn(String.format(Locale.ENGLISH, "Exception when refresh setting %s.%n%s", sectionName, settingString), e);
                }
            }
        });
        this.latestSettings = latestSettings;
        this.lastModifiedAt = System.currentTimeMillis();
    }

    public ObjectMapper getObjectMapper() {
        return om;
    }
}
