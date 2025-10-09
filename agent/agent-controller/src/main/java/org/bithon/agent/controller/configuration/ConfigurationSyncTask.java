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

package org.bithon.agent.controller.configuration;


import org.bithon.agent.configuration.ConfigurationFormat;
import org.bithon.agent.configuration.ConfigurationManager;
import org.bithon.agent.configuration.source.PropertySource;
import org.bithon.agent.configuration.source.PropertySourceType;
import org.bithon.agent.controller.IAgentController;
import org.bithon.component.commons.concurrency.PeriodicTask;
import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.LoggerFactory;
import org.bithon.component.commons.utils.HashUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sync agent configuration from remote server and apply changes to the local configuration manager.
 *
 * @author frank.chen021@outlook.com
 */
public class ConfigurationSyncTask extends PeriodicTask {

    private static final ILogAdaptor log = LoggerFactory.getLogger(ConfigurationSyncTask.class);

    private final String appName;
    private final String env;
    private final IAgentController controller;

    public ConfigurationSyncTask(String appName, String env, IAgentController controller, Duration refreshInterval) {
        super("bithon-cfg-sync", refreshInterval, false);

        this.appName = appName;
        this.env = env;
        this.controller = controller;

        // Trigger re-retrieve on immediately once some values holding at the controller side change
        this.controller.refreshListener(this::runImmediately);
    }

    @Override
    protected void onRun() {
        log.info("Sync agent configuration for {}-{} from remote", appName, env);

        // Get configuration from remote server
        Map<String, String> configurationListFromRemote = controller.getAgentConfiguration(appName, env, 0);
        if (configurationListFromRemote == null) {
            // Ignore internal error when invoke remote service
            // NOTE:
            // we need to process the empty map because the remote might delete all settings while the local still keeps these settings
            return;
        }

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
            String signature = HashUtils.sha256Hex(text);

            PropertySource existingPropertySource = existingSources.get(name);
            if (existingPropertySource == null
                || !signature.equals(existingPropertySource.getTag())) {

                PropertySource newSource = PropertySource.from(PropertySourceType.DYNAMIC,
                                                               name,
                                                               ConfigurationFormat.JSON,
                                                               text);
                newSource.setTag(signature);

                if (existingPropertySource == null) {
                    add.add(newSource);
                } else {
                    replace.put(name, newSource);
                }
            }
        }

        ConfigurationManager.getInstance()
                            .applyChanges(removed, replace, add);
    }

    @Override
    protected void onException(Exception e) {
        log.warn("Failed to update local configuration", e);
    }

    @Override
    protected void onStopped() {
        log.info("Task {} stopped.", getName());
    }
}
