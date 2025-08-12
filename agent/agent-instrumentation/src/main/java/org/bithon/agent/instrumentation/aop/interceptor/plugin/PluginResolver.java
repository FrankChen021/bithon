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

import java.io.DataInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/18-20:36
 */
public abstract class PluginResolver {

    private static final ILogger LOG = LoggerFactory.getLogger(PluginResolver.class);

    // Class-level variable to hold all interceptor type mappings
    private final Map<String, InterceptorType> interceptorTypes = new HashMap<>();

    public PluginResolver() {
        // create plugin class loader first
        PluginClassLoader.createClassLoader();
    }

    public Descriptors resolveInterceptors() {
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
        JarClassLoader pluginClassLoader = (JarClassLoader) PluginClassLoader.getClassLoader();
        return pluginClassLoader.getJars()
                                .stream()
                                .flatMap(JarFile::stream)
                                // Find the plugin class, which must be located under org.bithon.agent.plugin package
                                .filter(jarEntry -> jarEntry.getName().startsWith("org/bithon/agent/plugin/") && jarEntry.getName().endsWith("Plugin.class"))
                                // Load plugins by its alphabetic names
                                .sorted(Comparator.comparing(JarEntry::getName))
                                // Load plugin
                                .map((jarEntry) -> loadPlugin(jarEntry.getName(), pluginClassLoader))
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList());
    }

    private IPlugin loadPlugin(String pluginJarEntryName, JarClassLoader pluginClassLoader) {
        String pluginFullClassName = pluginJarEntryName.substring(0, pluginJarEntryName.length() - ".class".length())
                                                       .replace('/', '.');

        // A readable name for this plugin, so that users can use this name to disable one plugin
        // Since the above method already makes sure that the plugin class located under org.bithon.agent.plugin package,
        // we can directly use substring to get the name
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

            Map<String, InterceptorType> types = loadGeneratedInterceptorTypes(pluginName);
            interceptorTypes.putAll(types);

            LOG.info("Found plugin [{}] with [{}] interceptors", pluginName, types.size());

            return plugin;
        } catch (ClassFormatError t) {
            //
            // Some plugins only works for a specific JDK version, when the plugin is not compatible with the current JDK,
            // we give a more clear information about it
            //
            int classFileMajorVersion = -1;
            try (DataInputStream dataInputStream = new DataInputStream(pluginClassLoader.getResourceAsStream(pluginJarEntryName))) {
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
     * Load the generated interceptor types for a plugin.
     * The generated class is created by {@link org.bithon.agent.instrumentation.aop.interceptor.processor.InterceptorTypeProcessor} during compilation.
     */
    private static Map<String, InterceptorType> loadGeneratedInterceptorTypes(String pluginName) {
        try {
            String mappingClassName = "org.bithon.agent.plugin." + pluginName + ".InterceptorTypes";
            Class<?> mappingClass = Class.forName(mappingClassName, true, PluginClassLoader.getClassLoader());
            Method getTypesMethod = mappingClass.getMethod("getTypes");

            // Invoke the static method to get the interceptor types
            //noinspection unchecked
            return (Map<String, InterceptorType>) getTypesMethod.invoke(null);
        } catch (ClassNotFoundException e) {
            LOG.warn("No interceptor types found for plugin [{}]. Please report it to agent maintainers.", pluginName);
            return Collections.emptyMap();
        } catch (Exception e) {
            LOG.warn("Failed to load interceptor types for plugin [{}]: {}. Please report it to agent maintainers", pluginName, e.getMessage());
            return Collections.emptyMap();
        }
    }

    protected abstract boolean onResolved(Class<?> pluginClazz);
}
