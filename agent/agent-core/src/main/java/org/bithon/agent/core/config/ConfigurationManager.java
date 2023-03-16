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

package org.bithon.agent.core.config;

import org.bithon.agent.bootstrap.expt.AgentException;
import org.bithon.agent.bootstrap.utils.AgentDirectory;
import org.bithon.agent.core.bytecode.ClassDelegation;
import org.bithon.agent.core.bytecode.IDelegation;
import org.bithon.agent.core.context.AgentContext;
import org.bithon.component.commons.utils.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static java.io.File.separator;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/1/5 22:20
 */
public class ConfigurationManager {

    private static ConfigurationManager INSTANCE;

    public static ConfigurationManager getInstance() {
        return INSTANCE;
    }

    public static ConfigurationManager create() {
        File staticConfig = AgentDirectory.getSubDirectory(AgentDirectory.CONF_DIR + separator + "agent.yml");
        try (FileInputStream is = new FileInputStream(staticConfig)) {
            INSTANCE = new ConfigurationManager(Configuration.create(staticConfig.getAbsolutePath(),
                                                                     is,
                                                                     "bithon.",
                                                                     AgentContext.BITHON_APPLICATION_NAME,
                                                                     AgentContext.BITHON_APPLICATION_ENV));
            return INSTANCE;
        } catch (FileNotFoundException e) {
            throw new AgentException("Unable to find static config at [%s]", staticConfig.getAbsolutePath());
        } catch (IOException e) {
            throw new AgentException("Unexpected IO exception occurred: %s", e.getMessage());
        }
    }

    /**
     * for test only
     */
    static ConfigurationManager create(Configuration configuration) {
        INSTANCE = new ConfigurationManager(configuration);
        return INSTANCE;
    }

    private final Configuration configuration;
    /**
     * the value in the map is actually a dynamic type of {@link IDelegation}
     */
    private final Map<String, Object> delegatedBeans = new ConcurrentHashMap<>(19);

    private ConfigurationManager(Configuration configuration) {
        this.configuration = configuration;
    }

    /**
     * @return false is the plugin is disabled by configuration
     */
    public boolean addPluginConfiguration(Class<?> pluginClass) {
        Configuration pluginConfiguration = PluginConfiguration.load(pluginClass);
        if (!pluginConfiguration.isEmpty()) {
            String pluginConfigurationPrefix = PluginConfiguration.getPluginConfigurationPrefixName(pluginClass.getName());

            Boolean isPluginDisabled = pluginConfiguration.getConfig(pluginConfigurationPrefix + ".disabled", Boolean.class);
            if (isPluginDisabled != null && isPluginDisabled) {
                return false;
            }
        }

        // Merge the plugin configuration into agent configuration first so that the plugin initialization can obtain its configuration
        configuration.merge(pluginConfiguration);

        return true;
    }

    /**
     * Refresh configuration and reflect configuration changes to binding beans that support dynamic.
     *
     * @param newConfiguration incremental new configuration
     */
    public Set<String> refresh(Configuration newConfiguration) {
        // Replace the configuration and get the diff
        this.configuration.replace(newConfiguration);

        Set<String> changedKeys = newConfiguration.getKeys();

        // Re-bind the based on changes
        List<String> beanPrefixList = new ArrayList<>(delegatedBeans.keySet());
        for (String beanPrefix : beanPrefixList) {

            // If any changed key matches the bean configuration prefix,
            // we will reload the whole configuration
            for (String changedPrefix : changedKeys) {
                if (changedPrefix.startsWith(beanPrefix)) {
                    IDelegation delegatedObject = (IDelegation) delegatedBeans.get(beanPrefix);

                    // Create a new configuration object
                    Object newValue = getConfig(beanPrefix,
                                                // the delegated object is a generated class which inherits from real configuration class
                                                delegatedObject.getDelegationClass(),
                                                false);

                    // Update delegation
                    delegatedObject.setDelegation(newValue);
                    break;
                }
            }
        }

        return changedKeys;
    }

    /**
     * Bind configuration to an object. And if configuration changes, it will reflect on this object
     */
    public <T> T getConfig(Class<T> clazz) {
        ConfigurationProperties cfg = clazz.getAnnotation(ConfigurationProperties.class);
        if (cfg == null || StringUtils.isEmpty(cfg.prefix())) {
            throw new AgentException("Class [%s] does not have valid ConfigurationProperties.", clazz.getName());
        }

        return getConfig(cfg.prefix(), clazz, cfg.dynamic());
    }

    public <T> T getDynamicConfig(String prefix, Class<T> clazz) {
        return getConfig(prefix, clazz, true);
    }

    /**
     * Bind configuration to an object. And if configuration changes, it will reflect on this object
     */
    public <T> T getConfig(String prefixes, Class<T> clazz) {
        return getConfig(prefixes, clazz, false);
    }

    @SuppressWarnings("unchecked")
    public <T> T getConfig(String prefixes, Class<T> clazz, boolean isDynamic) {
        if (clazz.isPrimitive() || !isDynamic) {
            return configuration.getConfig(prefixes, clazz);
        }

        // If this configuration clazz is defined as dynamic(it means configuration changes will dynamically reflect on its corresponding configuration clazz object),
        // a delegation class is created
        return (T) delegatedBeans.computeIfAbsent(prefixes, (k) -> {
            Class<?> proxyClass = ClassDelegation.create(clazz);

            T val = configuration.getConfig(prefixes, clazz);
            try {
                return proxyClass.getConstructor(clazz).newInstance(val);
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e.getCause());
            }
        });
    }

    public String format(String format, boolean prettyFormat) {
        return configuration.format(format, prettyFormat);
    }
}
