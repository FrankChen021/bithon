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

package org.bithon.agent.configuration;

import org.bithon.agent.instrumentation.bytecode.ClassDelegation;
import org.bithon.agent.instrumentation.bytecode.IDelegation;
import org.bithon.agent.instrumentation.expt.AgentException;
import org.bithon.agent.instrumentation.utils.AgentDirectory;
import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.LoggerFactory;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.shaded.com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.bithon.shaded.com.fasterxml.jackson.databind.node.ObjectNode;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static java.io.File.separator;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/1/5 22:20
 */
public class ConfigurationManager {

    private static final ILogAdaptor log = LoggerFactory.getLogger(ConfigurationManager.class);

    public static final String BITHON_APPLICATION_ENV = "application.env";
    public static final String BITHON_APPLICATION_NAME = "application.name";

    private static ConfigurationManager INSTANCE;

    public static ConfigurationManager getInstance() {
        return INSTANCE;
    }

    private final Configuration externalConfiguration;

    public Configuration getExternalConfiguration() {
        return externalConfiguration;
    }

    /**
     * The configurations take effect by the following order:
     * 1. The built-in configuration file located at agent-distribution/conf/agent.yml
     * 2. User specified configuration file through -Dbithon.application.conf parameter
     * 3. Any command line parameters through -Dbithon.
     * 4. Any environment variables (Currently only application.name and application.env are supported)
     */
    public static synchronized ConfigurationManager create() {
        if (INSTANCE == null) {
            Configuration external = fromExternalConfiguration();

            Configuration configuration = fromDefaultConfiguration()
                // Use external configuration to overwrite the default configuration
                .merge(external)
                // Use the dynamic configuration to overwrite static configurations from file
                .merge(Configuration.fromCommandLineArgs("bithon."))
                // Use environment variables to overwrite previous ones
                .merge(Configuration.fromEnvironmentVariables("bithon."));

            INSTANCE = new ConfigurationManager(configuration, external);
        }
        return INSTANCE;
    }

    private static Configuration fromDefaultConfiguration() {
        return Configuration.from(AgentDirectory.getSubDirectory(AgentDirectory.CONF_DIR + separator + "agent.yml").getAbsolutePath(), true);
    }

    private static Configuration fromExternalConfiguration() {
        String locationName = "bithon.configuration.location";

        // Check if it's specified by command line args
        Properties properties = Configuration.fromCommandlineArgs(locationName);
        if (properties.isEmpty()) {
            return new Configuration(new ObjectNode(new JsonNodeFactory(true)));
        }

        // Check if it's specified by environment variables
        String configurationLocation = properties.getProperty(locationName);
        if (configurationLocation == null) {
            return new Configuration(new ObjectNode(new JsonNodeFactory(true)));
        }

        return Configuration.from(configurationLocation, true);
    }

    /**
     * For test only
     */
    static ConfigurationManager create(Configuration configuration) {
        INSTANCE = new ConfigurationManager(configuration, new Configuration());
        return INSTANCE;
    }

    private final Configuration configuration;
    /**
     * the value in the map is actually a dynamic type of {@link IDelegation}
     */
    private final Map<String, Object> delegatedBeans = new ConcurrentHashMap<>(13);

    private final Map<String, IConfigurationChangedListener> listeners = new ConcurrentHashMap<>(13);

    private ConfigurationManager(Configuration configuration, Configuration external) {
        this.configuration = configuration;
        this.externalConfiguration = external;
    }

    public void addConfigurationChangedListener(String keyPrefix, IConfigurationChangedListener listener) {
        listeners.put(keyPrefix, listener);
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

        // Re-bind the values based on changes
        List<String> beanPrefixList = new ArrayList<>(delegatedBeans.keySet());
        for (String beanPrefix : beanPrefixList) {

            // If any changed key matches the bean configuration prefix,
            // we will reload the whole configuration
            for (String changedPrefix : changedKeys) {
                if (changedPrefix.startsWith(beanPrefix)) {
                    IDelegation delegatedObject = (IDelegation) delegatedBeans.get(beanPrefix);

                    // Create a new configuration object
                    Object newValue = getConfig(beanPrefix,
                                                // the delegated object is a generated class
                                                // that inherits from real configuration class
                                                delegatedObject.getDelegationClass(),
                                                false);

                    // Update delegation
                    delegatedObject.setDelegation(newValue);
                    break;
                }
            }
        }

        //
        // Notify listeners about changes
        //
        for (Map.Entry<String, IConfigurationChangedListener> entry : listeners.entrySet()) {
            String watchedKey = entry.getKey();
            IConfigurationChangedListener listener = entry.getValue();

            if (changedKeys.contains(watchedKey)) {
                try {
                    listener.onChange();
                } catch (Exception e) {
                    log.warn("Exception when notify configuration change", e);
                }
            }
        }

        return changedKeys;
    }

    /**
     * Bind configuration to an object. And if the configuration changes, it will reflect on this object.
     * NOTE: The clazz must have a default ctor if it's annotated by {@link ConfigurationProperties}
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
     * Bind configuration to an object. And if the configuration changes, it will reflect on this object
     */
    public <T> T getConfig(String prefixes, Class<T> clazz) {
        return getConfig(prefixes, clazz, false);
    }

    @SuppressWarnings("unchecked")
    public <T> T getConfig(String prefixes, Class<T> clazz, boolean isDynamic) {
        if (clazz.isPrimitive() || !isDynamic) {
            return configuration.getConfig(prefixes, clazz);
        }

        // If this configuration clazz is defined as dynamic (it means configuration changes will dynamically reflect on its corresponding configuration clazz object),
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

    public void merge(Configuration configuration) {
        this.configuration.merge(configuration);
    }

    public String format(String format, boolean prettyFormat) {
        return configuration.format(format, prettyFormat);
    }
}
