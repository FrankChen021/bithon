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
import org.bithon.agent.instrumentation.utils.JdkUtils;

import java.io.File;
import java.util.Collection;
import java.util.Comparator;
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

    public PluginResolver() {
        // create plugin class loader first
        PluginClassLoader.createClassLoader();
    }

    public Descriptors resolveInterceptors() {
        // Load all interceptor types from the merged properties file first
        PluginMetadata pluginMeta = PluginMetadata.Loader.load(new File(AgentDirectory.getSubDirectory("plugins"), "plugins.meta"));

        // Load plugins
        List<IPlugin> plugins = loadPlugins(pluginMeta.pluginInfoList);

        Descriptors descriptors = new Descriptors();
        for (IPlugin plugin : plugins) {
            String pluginName = plugin.getClass().getSimpleName();

            // Merge class transformers
            descriptors.merge(plugin.getBithonClassDescriptor(), plugin.getPreconditions());

            // Merge method point cuts
            descriptors.merge(pluginName, plugin.getPreconditions(), plugin.getInterceptors());
        }

        // Resolve interceptor types for all method point cuts
        resolveInterceptorType(descriptors.getAllDescriptor(), pluginMeta.interceptorTypes);

        return descriptors;
    }

    private List<IPlugin> loadPlugins(List<PluginMetadata.PluginInfo> pluginInfoList) {
        // If we have known plugin classes from the INI file, use them directly
        if (pluginInfoList.isEmpty()) {
            throw new AgentException("Can't find plugins. Please report it to agent maintainers. ");
        }

        JarClassLoader pluginClassLoader = (JarClassLoader) PluginClassLoader.getClassLoader();
        return pluginInfoList.stream()
                             .sorted(Comparator.comparing(p -> p.className)) // Load plugins by alphabetic order
                             .map(pluginInfo -> loadPluginByClassName(pluginInfo, pluginClassLoader))
                             .filter(Objects::nonNull)
                             .collect(Collectors.toList());
    }

    /**
     * Load plugin by class name (optimized path when using INI file)
     */
    private IPlugin loadPluginByClassName(PluginMetadata.PluginInfo pluginInfo, JarClassLoader pluginClassLoader) {
        String pluginFullClassName = pluginInfo.className;
        // A readable name for this plugin, so that users can use this name to disable one plugin
        String packageName = pluginFullClassName.substring(0, pluginFullClassName.lastIndexOf('.'));
        String pluginName = packageName.substring("org.bithon.agent.plugin.".length());

        // Check if plugin has a minimal JDK version requirement
        if (JdkUtils.CURRENT_JAVA_VERSION < pluginInfo.minimalJdkVersion) {
            LOG.info("Found plugin [{}], but skipped because plugin requires JRE {} and above, current JRE is {}",
                     pluginName, pluginInfo.minimalJdkVersion,
                     JdkUtils.CURRENT_JAVA_VERSION);
            return null;
        }

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
            LOG.error("Found plugin [{}], but skipped due to unrecognizable plugin class file version: [{}]. Please report it to agent maintainers.", pluginName, t.getMessage());
            return null;
        } catch (Throwable e) {
            LOG.error(String.format(Locale.ENGLISH, "Failed to load plugin [%s]. Please report it to agent maintainers.", pluginName), e);
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

    protected abstract boolean onResolved(Class<?> pluginClazz);
}
