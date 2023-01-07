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

package org.bithon.agent.controller.config;


import org.bithon.agent.controller.IAgentController;
import org.bithon.agent.core.config.Configuration;
import org.bithon.agent.core.config.ConfigurationManager;
import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.LoggerFactory;
import org.bithon.component.commons.security.HashGenerator;
import org.bithon.component.commons.utils.CollectionUtils;
import org.bithon.shaded.com.fasterxml.jackson.databind.DeserializationFeature;
import org.bithon.shaded.com.fasterxml.jackson.databind.JsonNode;
import org.bithon.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dynamic Setting Manager for Plugins
 *
 * @author frank.chen021@outlook.com
 */
public class DynamicConfigurationManager {
    private static final ILogAdaptor log = LoggerFactory.getLogger(DynamicConfigurationManager.class);

    private static DynamicConfigurationManager INSTANCE = null;

    private final String appName;
    private final String env;
    private final IAgentController controller;
    private final Map<String, List<IAgentSettingRefreshListener>> listeners;
    private Long lastModifiedAt = 0L;
    private Map<String, JsonNode> latestSettings = Collections.emptyMap();
    private final Map<String, String> configSignatures = new HashMap<>();

    private final ObjectMapper om;

    private DynamicConfigurationManager(String appName, String env, IAgentController controller) {
        this.appName = appName;
        this.env = env;
        this.controller = controller;
        this.listeners = new ConcurrentHashMap<>();
        this.om = new ObjectMapper();
        om.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.controller.attachCommands(new ConfigCommandImpl());
    }

    public static void createInstance(String appName, String env, IAgentController controller) {
        INSTANCE = new DynamicConfigurationManager(appName, env, controller);
        INSTANCE.startPeriodicallyFetch();
    }

    public static DynamicConfigurationManager getInstance() {
        return INSTANCE;
    }

    public Map<String, JsonNode> getLatestSettings() {
        return latestSettings;
    }

    public void register(String name, IAgentSettingRefreshListener listener) {
        listeners.computeIfAbsent(name, key -> new ArrayList<>()).add(listener);
    }

    private void startPeriodicallyFetch() {
        if (controller != null) {
            new Timer("setting-fetcher").schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        retrieveConfigurations();
                    } catch (Throwable e) {
                        log.error("Failed to fetch plugin settings", e);
                    }
                }
            }, 3000, 60 * 1000);
        }
    }

    private void retrieveConfigurations() throws IOException {
        log.info("Fetch configuration for {}-{}", appName, env);

        // Get configuration from remote server
        Map<String, String> configurations = controller.fetch(appName, env, lastModifiedAt);
        if (CollectionUtils.isEmpty(configurations)) {
            return;
        }

        Map<String, JsonNode> latestSettings = new HashMap<>();
        for (Map.Entry<String, String> entry : configurations.entrySet()) {
            String name = entry.getKey();
            String text = entry.getValue();

            // Compare signature to determine if the configuration changes
            String signature = HashGenerator.sha256Hex(text);
            if (configSignatures.getOrDefault(name, "").equals(signature)) {
                continue;
            }

            log.info("Refresh configuration [{}]", name);
            configSignatures.put(name, signature);

            try (InputStream is = new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8))) {
                JsonNode configNode = Configuration.readStaticConfiguration(".json", is);

                // Refresh global configuration
                ConfigurationManager.getInstance().refresh(new Configuration(configNode));

                List<IAgentSettingRefreshListener> listeners = this.listeners.get(name);
                if (CollectionUtils.isEmpty(listeners)) {
                    continue;
                }
                for (IAgentSettingRefreshListener listener : listeners) {
                    try {
                        listener.onRefresh(om, configNode);
                    } catch (Exception e) {
                        log.warn(String.format(Locale.ENGLISH, "Exception when refresh setting %s.%n%s", name, text), e);
                    }
                }
            }
        }

        Set<String> notExistConfigs = new HashSet<>(configSignatures.keySet());
        notExistConfigs.removeAll(configurations.keySet());

        // TODO: Remove

        this.latestSettings = latestSettings;
        this.lastModifiedAt = System.currentTimeMillis();
    }

    public ObjectMapper getObjectMapper() {
        return om;
    }
}
