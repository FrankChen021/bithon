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


import org.bithon.agent.configuration.ConfigurationFormat;
import org.bithon.agent.configuration.ConfigurationManager;
import org.bithon.agent.configuration.source.PropertySource;
import org.bithon.agent.configuration.source.PropertySourceType;
import org.bithon.agent.controller.IAgentController;
import org.bithon.component.commons.concurrency.PeriodicTask;
import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.LoggerFactory;
import org.bithon.component.commons.security.HashGenerator;
import org.bithon.component.commons.utils.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dynamic Setting Manager for Plugins
 *
 * @author frank.chen021@outlook.com
 */
public class AgentSettingFetchTask extends PeriodicTask {

    private static final ILogAdaptor log = LoggerFactory.getLogger(AgentSettingFetchTask.class);

    private final String appName;
    private final String env;
    private final IAgentController controller;

    /**
     * key: configuration name in configuration storage. Has no meaning at agent side
     * val: configuration text
     */
    private final Map<String, String> configSignatures = new HashMap<>();


    public AgentSettingFetchTask(String appName, String env, IAgentController controller, Duration refreshInterval) {
        super("bithon-cfg-updater", refreshInterval, false);

        this.appName = appName;
        this.env = env;
        this.controller = controller;

        // Trigger re-retrieve on immediately once some values holding at the controller side change
        this.controller.refreshListener(this::runImmediately);
    }

    @Override
    protected void onRun() throws Exception {
        log.info("Fetch configuration for {}-{}", appName, env);

        // Get configuration from remote server
        Map<String, String> configurationListFromRemote = controller.getAgentConfiguration(appName, env, 0);

        List<String> removed = new ArrayList<>();
        Map<String, PropertySource> replace = new HashMap<>();
        List<PropertySource> add = new ArrayList<>();

        // Remove the configuration source from the local manager if the remote does not contain it
        Map<String, PropertySource> existingSources = ConfigurationManager.getInstance()
                                                                          .getPropertySource(PropertySourceType.DYNAMIC);
        for (String name : existingSources.keySet()) {
            if (!configurationListFromRemote.containsKey(name)) {
                // The local dynamic configuration no longer exists in the remote
                // We need to remove it
                removed.add(name);
            }
        }

        // Check if the one has been deleted from the remote
        for (Map.Entry<String, String> entry : configurationListFromRemote.entrySet()) {
            String name = entry.getKey();
            String text = entry.getValue();

            // Generate signature for further comparison
            String signature = HashGenerator.sha256Hex(text);

            PropertySource existingPropertySource = existingSources.get(name);
            if (existingPropertySource == null
                || !signature.equals(existingPropertySource.getTag())) {

                try (InputStream is = new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8))) {
                    PropertySource newSource = PropertySource.from(PropertySourceType.DYNAMIC,
                                                                   name,
                                                                   ConfigurationFormat.JSON,
                                                                   is);
                    newSource.setTag(signature);

                    if (existingPropertySource == null) {
                        add.add(newSource);
                    } else {
                        replace.put(name, newSource);
                    }
                } catch (IOException e) {
                    log.error(StringUtils.format("Error to deserialize configuration "));
                }
            }
        }

        ConfigurationManager.getInstance()
                            .applyChanges(removed, replace, add);
    }

    @Override
    protected void onException(Exception e) {
        log.error("Failed to update local configuration", e);
    }

    @Override
    protected void onStopped() {
        log.info("Task {} stopped.", getName());
    }
}
