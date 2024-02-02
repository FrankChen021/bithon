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


import org.bithon.agent.configuration.Configuration;
import org.bithon.agent.configuration.ConfigurationManager;
import org.bithon.agent.controller.IAgentController;
import org.bithon.component.commons.concurrency.PeriodicTask;
import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.LoggerFactory;
import org.bithon.component.commons.security.HashGenerator;
import org.bithon.component.commons.utils.CollectionUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Dynamic Setting Manager for Plugins
 *
 * @author frank.chen021@outlook.com
 */
public class DynamicConfigurationManager {
    private static final ILogAdaptor log = LoggerFactory.getLogger(DynamicConfigurationManager.class);

    private final String appName;
    private final String env;
    private final IAgentController controller;

    private final PeriodicTask updateConfigTask;

    private DynamicConfigurationManager(String appName, String env, IAgentController controller) {
        this.appName = appName;
        this.env = env;
        this.controller = controller;
        this.updateConfigTask = new UpdateConfigurationTask();

        // Attach service on this channel
        this.controller.attachCommands(new ConfigurationCommandImpl());

        // Trigger re-retrieve on immediately once some values holding at the controller side change
        this.controller.refreshListener(updateConfigTask::runImmediately);
    }

    public static void createInstance(String appName, String env, IAgentController controller) {
        DynamicConfigurationManager manager = new DynamicConfigurationManager(appName, env, controller);
        manager.updateConfigTask.start();
    }

    private class UpdateConfigurationTask extends PeriodicTask {
        private Long lastModifiedAt = 0L;

        /**
         * key: configuration name in configuration storage. Has no meaning at agent side
         * val: configuration text
         */
        private final Map<String, String> configSignatures = new HashMap<>();

        public UpdateConfigurationTask() {
            super("bithon-cfg-updater", Duration.ofMinutes(1), true);
        }

        @Override
        protected void onRun() throws Exception {
            log.info("Fetch configuration for {}-{}", appName, env);

            // Get configuration from remote server
            Map<String, String> configurations = controller.getAgentConfiguration(appName, env, lastModifiedAt);
            if (CollectionUtils.isEmpty(configurations)) {
                return;
            }

            Configuration config = null;
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
                    Configuration cfg = Configuration.from(".json", is);

                    if (config == null) {
                        config = cfg;
                    } else {
                        config.merge(cfg);
                    }
                }
            }
            if (config == null) {
                return;
            }

            // Update saved configuration
            // TODO: incremental configuration deletion is not supported because of complexity.
            // if the incremental configuration overwrites the global configuration, we need to restore or we can't delete the configuration directly
            // One solution is that the 'refresh' method below return the action on each returned key, ADD/REPLACE,
            // and then we keep the changed keys and corresponding actions for rollback.
            // Since it's not a must-have feature at this stage, leave it to future when we really needs it.
            Set<String> changedKeys = ConfigurationManager.getInstance().refresh(config);
            if (changedKeys.isEmpty()) {
                return;
            }

            lastModifiedAt = System.currentTimeMillis();
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
}
