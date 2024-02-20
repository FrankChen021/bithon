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

import org.bithon.agent.configuration.source.CommandLineArgsConfiguration;
import org.bithon.agent.configuration.source.ConfigurationSource;
import org.bithon.agent.configuration.source.EnvironmentConfiguration;
import org.bithon.agent.configuration.source.ExternalConfiguration;
import org.bithon.agent.instrumentation.bytecode.ClassDelegation;
import org.bithon.agent.instrumentation.bytecode.IDelegation;
import org.bithon.agent.instrumentation.expt.AgentException;
import org.bithon.agent.instrumentation.utils.AgentDirectory;
import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.LoggerFactory;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.shaded.com.fasterxml.jackson.databind.JsonNode;
import org.bithon.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.bithon.shaded.com.fasterxml.jackson.databind.node.NullNode;
import org.bithon.shaded.com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static java.io.File.separator;

/**
 * <pre>
 * The ConfigurationManager manages all configuration from different places, including
 * 1. The default agent configuration located under the agent-distribution/conf
 * 2. The external configuration specified via -Dbithon.configuration.location command line argument
 * 3. All parameters defined by -Dbithon.xxx
 * 4. All environment variables starting with bithon_
 * 5. Dynamic configuration from the remote controller
 *
 * Different configuration sources may contain the same property keys,
 * in such a case, the latter one in the above list overwrites the previous one.
 * For Dynamic configurations, the order is determined by its name in ascending alphabetic order.
 *
 * Internal implementation:
 * 1. When getting configuration properties,
 * the manager will collect properties with the same key prefixes from all sources and merge them in the above list order.
 *
 * Let's us p1 has the following configuration defined(Note it's just example, not reflected real configuration):
 * <pre>
 * -- p1
 *   tracing:
 *     samplingRate:
 *       grpc: 100%
 *       headers: ["user-agent"]
 * </pre>
 * <p>
 * And p2 has the following configuration:
 * <pre>
 * -- p2
 *   tracing:
 *     samplingRate:
 *       http: 100%
 *       headers: ["x-forwarded-for"]
 * </pre>
 * <p>
 * When getting properties by 'tracing.samplingRate',
 * both grpc and http properties will be collected from the two sources as they don't conflict with each other.
 * However, the for headers property, as it's defined in both sources, only one will be used. Which one will be used is determined by the priority of these two sources
 * as described above.
 * For example, if p1 is placed in the default agent.yml, it has lower priority, that means the value of headers property in p2 will be used.
 * <p>
 * 2. When a configuration source is deleted,
 * ALL properties(including its prefix) in the deleted configuration will be notified as CHANGED
 * </pre>
 *
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

    /**
     * The configurations take effect by the following order:
     * 1. The built-in configuration file located at agent-distribution/conf/agent.yml
     * 2. User specified configuration file through -Dbithon.application.conf parameter
     * 3. Any command line parameters through -Dbithon.
     * 4. Any environment variables (Currently only application.name and application.env are supported)
     */
    public static synchronized ConfigurationManager create() {
        if (INSTANCE == null) {
            INSTANCE = create(AgentDirectory.getSubDirectory(AgentDirectory.CONF_DIR + separator + "agent.yml"));
        }
        return INSTANCE;
    }

    /**
     * Exposed for testing
     */
    static ConfigurationManager create(File defaultConfigLocation) {
        return new ConfigurationManager(Configuration.from(ConfigurationSource.INTERNAL, defaultConfigLocation, true),
                                        ExternalConfiguration.build(),
                                        CommandLineArgsConfiguration.build("bithon."),
                                        EnvironmentConfiguration.build("bithon."));
    }

    // Sorted in priority
    private final List<Configuration> configurations = new ArrayList<>();

    /**
     * the value in the map is actually a dynamic type of {@link IDelegation}
     */
    private final Map<String, IDelegation> mappedBeans = new ConcurrentHashMap<>(13);

    private final Map<String, IConfigurationChangedListener> listeners = new ConcurrentHashMap<>(13);

    private ConfigurationManager(Configuration... configurations) {
        for (Configuration configuration : configurations) {
            if (configuration != null) {
                this.configurations.add(configuration);
            }
        }
        Collections.sort(this.configurations);
    }

    public void addConfigurationChangedListener(String keyPrefix, IConfigurationChangedListener listener) {
        listeners.put(keyPrefix, listener);
    }

    public void addConfiguration(Configuration newConfiguration) {

        Set<String> changedKeys = new HashSet<>();

        boolean found = false;
        synchronized (this.configurations) {
            for (Configuration configuration : this.configurations) {
                if (configuration.getName().equals(newConfiguration.getName())
                    && configuration.getSource() == newConfiguration.getSource()
                ) {
                    found = true;

                    // Replace configuration
                    configuration.swap(newConfiguration);

                    changedKeys.addAll(newConfiguration.getKeys());
                    changedKeys.addAll(configuration.getKeys());

                    break;
                }
            }
            if (!found) {
                this.configurations.add(newConfiguration);
                Collections.sort(this.configurations);

                changedKeys.addAll(newConfiguration.getKeys());
            }
        }

        if (!changedKeys.isEmpty()) {
            this.applyChanges(changedKeys);
        }
    }

    /**
     * Bind configuration to an object. And if the configuration changes, it will reflect on this object.
     * NOTE: The clazz must have a default ctor if it's annotated by {@link ConfigurationProperties}
     */
    public <T> T getConfig(Class<T> clazz) {
        ConfigurationProperties cfg = clazz.getAnnotation(ConfigurationProperties.class);
        if (cfg == null || StringUtils.isEmpty(cfg.path())) {
            throw new AgentException("Class [%s] does not have valid ConfigurationProperties.", clazz.getName());
        }

        return getConfig(cfg.path(), clazz, cfg.dynamic());
    }

    public <T> T getDynamicConfig(String prefix, Class<T> clazz) {
        return getConfig(prefix, clazz, true);
    }

    /**
     * Bind configuration to an object. And if the configuration changes, it will reflect on this object
     */
    public <T> T getConfig(String prefix, Class<T> clazz) {
        return getConfig(prefix, clazz, false);
    }

    @SuppressWarnings("unchecked")
    public <T> T getConfig(String prefix, Class<T> clazz, boolean isDynamic) {
        if (clazz.isPrimitive() || !isDynamic) {
            return collect(prefix).getConfig(prefix, clazz);
        }

        // If this configuration clazz is defined as dynamic (it means configuration changes will dynamically reflect on its corresponding configuration clazz object),
        // a delegation class is created
        return (T) mappedBeans.computeIfAbsent(prefix, (k) -> {
            Class<?> proxyClass = ClassDelegation.create(clazz);

            T val = collect(prefix).getConfig(prefix, clazz);
            try {
                return (IDelegation) proxyClass.getConstructor(clazz).newInstance(val);
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e.getCause());
            }
        });
    }

    /**
     * Collect properties that have the same given prefix from multiple sources
     */
    private Configuration collect(String propertyPath) {
        String[] propertyPaths = propertyPath.split("\\.");

        boolean isFirst = true;
        JsonNode node = null;
        Configuration[] configurationList = this.configurations.toArray(new Configuration[0]);
        for (Configuration configuration : configurationList) {
            JsonNode found = configuration.getConfigurationNode(propertyPaths);
            if (found == null || found instanceof NullNode) {
                continue;
            }
            if (node == null) {
                node = found;
            } else {
                if (isFirst) {
                    node = node.deepCopy();
                    isFirst = false;
                }
                Configuration.merge(node, found.deepCopy(), true);
            }
        }

        if (node == null || node instanceof NullNode) {
            return new Configuration(ConfigurationSource.DYNAMIC, "for-eval");
        }

        ObjectMapper om = new ObjectMapper();
        ObjectNode root = om.createObjectNode();
        ObjectNode parent = root;
        int i = 0;
        for (; i < propertyPaths.length - 1; i++) {
            ObjectNode next = om.createObjectNode();
            parent.set(propertyPaths[i], next);
            parent = next;
        }
        parent.set(propertyPaths[i], node);

        return new Configuration(ConfigurationSource.DYNAMIC, "for-eval", root);
    }

    public String getActiveConfiguration(String format, boolean prettyFormat) {
        Configuration active = null;
        Configuration[] configurationList = this.configurations.toArray(new Configuration[0]);
        for (Configuration configuration : configurationList) {
            if (active == null) {
                active = configuration.clone();
            } else {
                active.merge(configuration.clone());
            }
        }

        return active == null ? "" : active.format(format, prettyFormat);
    }

    public Map<String, Configuration> getConfiguration(ConfigurationSource source) {
        synchronized (this.configurations) {
            return this.configurations.stream()
                                      .filter((cfg) -> cfg.getSource() == source)
                                      .collect(Collectors.toMap(Configuration::getName, v -> v));
        }
    }

    public void applyChanges(List<String> removed,
                             Map<String, Configuration> replace,
                             List<Configuration> add) {
        Set<String> changedKeys = new HashSet<>();

        synchronized (this.configurations) {
            // Processing removing first
            if (!removed.isEmpty()) {
                for (Iterator<Configuration> i = this.configurations.iterator(); i.hasNext(); ) {
                    Configuration cfg = i.next();
                    if (cfg.getSource() == ConfigurationSource.DYNAMIC && removed.contains(cfg.getName())) {
                        i.remove();

                        changedKeys.addAll(cfg.getKeys());
                    }
                }
            }

            if (!replace.isEmpty()) {
                for (Configuration cfg : this.configurations) {
                    if (cfg.getSource() != ConfigurationSource.DYNAMIC) {
                        continue;
                    }

                    Configuration replacement = replace.get(cfg.getName());
                    if (replacement != null) {
                        cfg.swap(replacement);

                        changedKeys.addAll(cfg.getKeys());
                        changedKeys.addAll(replacement.getKeys());
                    }
                }
            }

            if (!add.isEmpty()) {
                for (Configuration cfg : add) {
                    this.configurations.add(cfg);
                    changedKeys.addAll(cfg.getKeys());
                }
            }

            Collections.sort(this.configurations);
        }

        // Apply Changes to mapped beans
        if (!changedKeys.isEmpty()) {
            applyChanges(changedKeys);
        }
    }

    private void applyChanges(Set<String> changedKeys) {
        //
        // Re-bind the values based on changes
        //
        for (Map.Entry<String, IDelegation> mapEntry : mappedBeans.entrySet()) {
            String beanPropertyPath = mapEntry.getKey();
            IDelegation bean = mapEntry.getValue();

            // If any changed key matches the bean configuration prefix,
            // we will reload the whole configuration
            for (String changedPath : changedKeys) {
                if (changedPath.startsWith(beanPropertyPath)) {
                    // Create a new configuration object
                    Object newValue = getConfig(beanPropertyPath,
                                                // the delegated object is a generated class
                                                // that inherits from real configuration class
                                                bean.getDelegationClass(),
                                                // No need to create a proxy for the config because the bean here is the proxy
                                                false);

                    // Update delegation
                    bean.setDelegation(newValue);

                    // Break to go on next bean
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
    }
}
