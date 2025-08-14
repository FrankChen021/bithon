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

package org.bithon.agent.instrumentation.aop.interceptor.plugin;

import org.bithon.agent.instrumentation.aop.interceptor.InterceptorType;
import org.bithon.agent.instrumentation.aop.interceptor.InterceptorTypeResolver;
import org.bithon.agent.instrumentation.aop.interceptor.descriptor.Descriptors;
import org.bithon.agent.instrumentation.aop.interceptor.descriptor.MethodPointCutDescriptor;
import org.bithon.agent.instrumentation.expt.AgentException;
import org.bithon.agent.instrumentation.loader.JarClassLoader;
import org.bithon.agent.instrumentation.loader.PluginClassLoader;
import org.bithon.agent.instrumentation.logging.ILogger;
import org.bithon.agent.instrumentation.logging.LoggerFactory;
import org.bithon.agent.instrumentation.utils.AgentDirectory;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/18-20:36
 */
public abstract class PluginResolver {

    private static final ILogger LOG = LoggerFactory.getLogger(PluginResolver.class);

    // Class-level variable to hold all interceptor type mappings
    private final Map<String, InterceptorType> interceptorTypes = new HashMap<>();
    private final List<String> pluginClassList = new ArrayList<>();

    public PluginResolver() {
        // create plugin class loader first
        PluginClassLoader.createClassLoader();
    }

    public Descriptors resolveInterceptors() {
        // Load all interceptor types from the merged properties file first
        loadPluginMetadata();

        // Load plugins
        List<IPlugin> plugins = loadPlugins();

        Descriptors descriptors = new Descriptors();
        for (IPlugin plugin : plugins) {
            String pluginName = plugin.getClass().getSimpleName();

            descriptors.merge(plugin.getBithonClassDescriptor(), plugin.getPreconditions());

            descriptors.merge(pluginName, plugin.getPreconditions(), plugin.getInterceptors());
        }

        resolveInterceptorType(descriptors.getAllDescriptor(), this.interceptorTypes);

        return descriptors;
    }

    /**
     * Resolve the interceptor type ({@link InterceptorType}) for each interceptor declared in each plugin
     * Using preloaded interceptor type mappings with fallback to runtime resolution
     */
    public static void resolveInterceptorType(Collection<Descriptors.Descriptor> descriptors, Map<String, InterceptorType> knowInterceptorTypes) {
        // A fallback if the generated
        InterceptorTypeResolver runtimeTypeResolver = new InterceptorTypeResolver(PluginClassLoader.getClassLoader());

        for (Descriptors.Descriptor descriptor : descriptors) {
            for (Descriptors.MethodPointCuts pointcut : descriptor.getMethodPointCuts()) {
                for (MethodPointCutDescriptor pointcutDescriptor : pointcut.getMethodInterceptors()) {
                    String interceptorClassName = pointcutDescriptor.getInterceptorClassName();

                    InterceptorType type = knowInterceptorTypes.get(interceptorClassName);
                    if (type != null) {
                        // Found in compile-time mapping
                        pointcutDescriptor.setInterceptorType(type);
                    } else {
                        // Fallback to runtime resolution
                        try {
                            type = runtimeTypeResolver.resolve(interceptorClassName);
                            pointcutDescriptor.setInterceptorType(type);
                            LOG.info("Resolved interceptor type for [{}] using runtime fallback: {}", interceptorClassName, type);
                        } catch (AgentException e) {
                            throw new AgentException("Unable to resolve interceptor type for [%s]: %s. Please report this to agent maintainers.", interceptorClassName, e.getMessage());
                        }
                    }
                }
            }
        }
    }

    private List<IPlugin> loadPlugins() {
        // If we have known plugin classes from the INI file, use them directly
        if (pluginClassList.isEmpty()) {
            throw new AgentException("Can't find plugins. Please report it to agent maintainers. ");
        }

        JarClassLoader pluginClassLoader = (JarClassLoader) PluginClassLoader.getClassLoader();
        return pluginClassList.stream()
                              .sorted() // Load plugins by alphabetic order
                              .map(pluginClassName -> loadPluginByClassName(pluginClassName, pluginClassLoader))
                              .filter(Objects::nonNull)
                              .collect(Collectors.toList());
    }

    /**
     * Load plugin by class name (optimized path when using INI file)
     */
    private IPlugin loadPluginByClassName(String pluginFullClassName, JarClassLoader pluginClassLoader) {
        // A readable name for this plugin, so that users can use this name to disable one plugin
        String packageName = pluginFullClassName.substring(0, pluginFullClassName.lastIndexOf('.'));
        String pluginName = packageName.substring("org.bithon.agent.plugin.".length());

        try {
            Class<?> pluginClass = Class.forName(pluginFullClassName, true, pluginClassLoader);
            if (!isPluginClass(pluginClass)) {
                LOG.warn("Resource [{}] is not type of IPlugin. The class name does not comply with the plugin standard. Please change its name.",
                         pluginFullClassName);
                return null;
            }

            if (!onResolved(pluginClass)) {
                LOG.info("Found plugin [{}], but it's DISABLED by configuration", pluginName);
                return null;
            }

            IPlugin plugin = (IPlugin) pluginClass.getDeclaredConstructor().newInstance();

            LOG.info("Found plugin [{}]", pluginName);

            return plugin;
        } catch (ClassFormatError t) {
            //
            // Some plugins only works for a specific JDK version, when the plugin is not compatible with the current JDK,
            // we give a more clear information about it
            //
            int classFileMajorVersion = -1;
            String jarEntryName = pluginFullClassName.replace('.', '/') + ".class";
            try (DataInputStream dataInputStream = new DataInputStream(pluginClassLoader.getResourceAsStream(jarEntryName))) {
                int magic = dataInputStream.readInt();
                if (magic == 0xCAFEBABE) {
                    // class file minor version
                    dataInputStream.readUnsignedShort();
                    classFileMajorVersion = dataInputStream.readUnsignedShort();
                }
            } catch (IOException ignored) {
            }
            if (classFileMajorVersion == -1) {
                LOG.error("Found plugin [{}], but skipped due to unrecognizable plugin class file version: [{}]", pluginName, t.getMessage());
            } else {
                LOG.info("Found plugin [{}], but skipped because plugin requires JDK {} and above",
                         pluginName,
                         classFileMajorVersion - 44);
            }
            return null;
        } catch (Throwable e) {
            LOG.error(String.format(Locale.ENGLISH, "Failed to load plugin [%s]", pluginName), e);
            return null;
        }
    }

    /**
     * A plugin must implement the {@link IPlugin} interface.
     * So we check if the given class directly or indirectly implements the interface.
     */
    private boolean isPluginClass(Class<?> clazz) {
        for (Class<?> inf : clazz.getInterfaces()) {
            if (inf.equals(IPlugin.class)) {
                return true;
            }
        }

        Class<?> parentClass = clazz.getSuperclass();
        return parentClass != null && isPluginClass(parentClass);
    }

    /**
     * Load all interceptor types and plugin classes from the external plugins.meta file.
     * The plugins.meta file is created during the agent-distribution build process and placed in the plugins directory.
     */
    private void loadPluginMetadata() {
        File pluginMetaFile = new File(AgentDirectory.getSubDirectory("plugins"), "plugins.meta");
        if (!pluginMetaFile.exists()) {
            throw new AgentException("Plugin metadata file not found. Please report it to agent maintainers.");
        }

        try (FileInputStream fileStream = new FileInputStream(pluginMetaFile)) {
            loadPluginMetadata(fileStream);
        } catch (IOException e) {
            throw new AgentException("Unable to read plugin metadata file: %s", e.getMessage());
        }
    }

    /**
     * Parse INI-style properties file to extract plugin classes and interceptor types.
     * Format:
     * [plugin.class.name]
     * interceptor.class.name=INTERCEPTOR_TYPE
     */
    private void loadPluginMetadata(InputStream inputStream) throws IOException {


        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // Skip comments and empty lines
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                // Find plugin class declaration as [plugin.class.name]
                if (line.startsWith("[") && line.endsWith("]")) {
                    String pluginClass = line.substring(1, line.length() - 1);
                    pluginClassList.add(pluginClass);
                    continue;
                }

                int equalIndex = line.indexOf('=');
                if (equalIndex > 0) {
                    String interceptorClassName = line.substring(0, equalIndex).trim();
                    String typeString = line.substring(equalIndex + 1).trim();

                    try {
                        interceptorTypes.put(interceptorClassName, InterceptorType.valueOf(typeString));
                    } catch (IllegalArgumentException e) {
                        LOG.warn("Invalid interceptor type [{}] for interceptor [{}]", typeString, interceptorClassName);
                    }
                }
            }
        }
    }

    protected abstract boolean onResolved(Class<?> pluginClazz);
}
