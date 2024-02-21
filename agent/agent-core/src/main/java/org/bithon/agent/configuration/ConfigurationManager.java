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

import org.bithon.agent.configuration.source.CommandLineArgsSource;
import org.bithon.agent.configuration.source.EnvironmentSource;
import org.bithon.agent.configuration.source.ExternalSource;
import org.bithon.agent.configuration.source.PropertySource;
import org.bithon.agent.configuration.source.PropertySourceType;
import org.bithon.agent.instrumentation.bytecode.IProxyObject;
import org.bithon.agent.instrumentation.bytecode.ProxyClassGenerator;
import org.bithon.agent.instrumentation.expt.AgentException;
import org.bithon.agent.instrumentation.utils.AgentDirectory;
import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.LoggerFactory;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.shaded.com.fasterxml.jackson.databind.JsonNode;
import org.bithon.shaded.com.fasterxml.jackson.databind.node.NullNode;

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
        return new ConfigurationManager(PropertySource.from(PropertySourceType.INTERNAL, defaultConfigLocation, true),
                                        ExternalSource.build(),
                                        CommandLineArgsSource.build("bithon."),
                                        EnvironmentSource.build("bithon_"));
    }

    // Sorted in priority
    private final List<PropertySource> propertySources = new ArrayList<>();

    /**
     * the value in the map is actually a dynamic type of {@link IProxyObject}
     */
    private final Map<String, IProxyObject> proxiedBeans = new ConcurrentHashMap<>(13);

    /**
     * key - the watched property path
     * val - listener to process change
     */
    private final Map<String, IConfigurationChangedListener> listeners = new ConcurrentHashMap<>(13);

    private ConfigurationManager(PropertySource... propertySources) {
        for (PropertySource propertySource : propertySources) {
            if (propertySource != null) {
                this.propertySources.add(propertySource);
            }
        }
        Collections.sort(this.propertySources);
    }

    public void addConfigurationChangedListener(String propertyPath, IConfigurationChangedListener listener) {
        listeners.put(propertyPath, listener);
    }

    public void addPropertySource(PropertySource newSource) {
        Set<String> changedKeys = new HashSet<>();

        boolean found = false;
        synchronized (this.propertySources) {
            for (PropertySource propertySource : this.propertySources) {
                if (propertySource.getName().equals(newSource.getName())
                    && propertySource.getType() == newSource.getType()
                ) {
                    found = true;

                    // Replace configuration
                    propertySource.swap(newSource);

                    changedKeys.addAll(newSource.getKeys());
                    changedKeys.addAll(propertySource.getKeys());

                    break;
                }
            }
            if (!found) {
                this.propertySources.add(newSource);
                Collections.sort(this.propertySources);

                changedKeys.addAll(newSource.getKeys());
            }
        }

        if (!changedKeys.isEmpty()) {
            this.applyChanges(changedKeys);
        }
    }

    /**
     * Bind configuration to an object.
     * And if the configuration changes, it WILL reflect on this object.
     * NOTE: The clazz must have a non-private default ctor if it's annotated by {@link ConfigurationProperties}
     */
    public <T> T getConfig(Class<T> clazz) {
        ConfigurationProperties cfg = clazz.getAnnotation(ConfigurationProperties.class);
        if (cfg == null || StringUtils.isEmpty(cfg.path())) {
            throw new AgentException("Class [%s] does not have valid ConfigurationProperties.", clazz.getName());
        }

        return getConfig(cfg.path(), clazz, cfg.dynamic());
    }

    public <T> T getDynamicConfig(String propertyPath, Class<T> clazz) {
        return getConfig(propertyPath, clazz, true);
    }

    /**
     * Bind configuration to an object. And if the configuration changes, it will NOT reflect on this object
     */
    public <T> T getConfig(String propertyPath, Class<T> clazz) {
        return getConfig(propertyPath, clazz, false);
    }

    @SuppressWarnings("unchecked")
    public <T> T getConfig(String propertyPath, Class<T> clazz, boolean isDynamic) {
        if (clazz.isPrimitive() || !isDynamic) {
            return Binder.bind(propertyPath, collect(propertyPath), clazz);
        }

        // If this configuration clazz is defined as dynamic (it means configuration changes will dynamically reflect on its corresponding configuration clazz object),
        // a delegation class is created
        return (T) proxiedBeans.computeIfAbsent(propertyPath, (k) -> {
            Class<?> proxyClass = ProxyClassGenerator.create(clazz);

            T val = Binder.bind(propertyPath, collect(propertyPath), clazz);
            try {
                // For each generated
                return (IProxyObject) proxyClass.getConstructor(clazz)
                                                .newInstance(val);
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e.getCause());
            }
        });
    }

    /**
     * Collect properties that have the same given prefix from multiple sources
     * @return A nullable node that contains all properties under the give property path
     */
    private JsonNode collect(String propertyPath) {
        String[] propertyPaths = propertyPath.split("\\.");

        boolean isFirst = true;
        JsonNode node = null;
        PropertySource[] propertySourceList = this.propertySources.toArray(new PropertySource[0]);
        for (PropertySource propertySource : propertySourceList) {
            JsonNode found = propertySource.getPropertyNode(propertyPaths);
            if (found == null || found instanceof NullNode) {
                continue;
            }
            if (node == null) {
                node = found;
            } else {
                // A configuration node is found on a new property source, then we need to merge them.
                // But before that, we need to make sure the source is a deep copy
                if (isFirst) {
                    node = node.deepCopy();
                    isFirst = false;
                }
                PropertySource.merge(node, found.deepCopy(), true);
            }
        }

        return node;
    }

    public String getActiveConfiguration(String format, boolean prettyFormat) {
        PropertySource active = null;
        PropertySource[] propertySourceList = this.propertySources.toArray(new PropertySource[0]);
        for (PropertySource propertySource : propertySourceList) {
            if (active == null) {
                active = propertySource.clone();
            } else {
                active.merge(propertySource.clone());
            }
        }

        return active == null ? "" : active.format(format, prettyFormat);
    }

    public Map<String, PropertySource> getPropertySource(PropertySourceType type) {
        synchronized (this.propertySources) {
            return this.propertySources.stream()
                                       .filter((cfg) -> cfg.getType() == type)
                                       .collect(Collectors.toMap(PropertySource::getName, v -> v));
        }
    }

    public void applyChanges(List<String> removed,
                             Map<String, PropertySource> replace,
                             List<PropertySource> add) {
        Set<String> changedKeys = new HashSet<>();

        synchronized (this.propertySources) {
            // Processing removing first
            if (!removed.isEmpty()) {
                for (Iterator<PropertySource> i = this.propertySources.iterator(); i.hasNext(); ) {
                    PropertySource cfg = i.next();
                    if (cfg.getType() == PropertySourceType.DYNAMIC && removed.contains(cfg.getName())) {
                        i.remove();

                        changedKeys.addAll(cfg.getKeys());
                    }
                }
            }

            if (!replace.isEmpty()) {
                for (PropertySource cfg : this.propertySources) {
                    if (cfg.getType() != PropertySourceType.DYNAMIC) {
                        continue;
                    }

                    PropertySource replacement = replace.get(cfg.getName());
                    if (replacement != null) {
                        cfg.swap(replacement);

                        changedKeys.addAll(cfg.getKeys());
                        changedKeys.addAll(replacement.getKeys());
                    }
                }
            }

            if (!add.isEmpty()) {
                for (PropertySource cfg : add) {
                    this.propertySources.add(cfg);
                    changedKeys.addAll(cfg.getKeys());
                }
            }

            Collections.sort(this.propertySources);
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
        for (Map.Entry<String, IProxyObject> mapEntry : proxiedBeans.entrySet()) {
            String beanPropertyPath = mapEntry.getKey();
            IProxyObject bean = mapEntry.getValue();

            // If any changed key matches the bean configuration prefix,
            // we will reload the whole configuration
            for (String changedPath : changedKeys) {
                if (changedPath.startsWith(beanPropertyPath)) {
                    // Create a new configuration object
                    Object newValue = getConfig(beanPropertyPath,
                                                // the delegated object is a generated class
                                                // that inherits from real configuration class
                                                bean.getProxyClass(),
                                                // No need to create a proxy for the config because the bean here is the proxy
                                                false);

                    // Update delegation
                    bean.setProxyObject(newValue);

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
