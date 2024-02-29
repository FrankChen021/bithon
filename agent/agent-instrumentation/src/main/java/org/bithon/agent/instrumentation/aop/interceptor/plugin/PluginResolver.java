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
import org.bithon.agent.instrumentation.loader.PluginClassLoaderManager;
import org.bithon.agent.instrumentation.logging.ILogger;
import org.bithon.agent.instrumentation.logging.LoggerFactory;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/18-20:36
 */
public abstract class PluginResolver {

    private final ILogger LOG = LoggerFactory.getLogger(PluginResolver.class);

    public PluginResolver() {
        // create plugin class loader first
        PluginClassLoaderManager.createDefault();
    }

    public Descriptors resolveInterceptors() {
        // Load plugins
        List<IPlugin> plugins = loadPlugins();

        Descriptors descriptors = new Descriptors();
        for (IPlugin plugin : plugins) {
            String pluginName = plugin.getClass().getSimpleName();

            descriptors.merge(plugin.getBithonClassDescriptor());

            descriptors.merge(pluginName, plugin.getPreconditions(), plugin.getInterceptors());
        }

        resolveInterceptorType(descriptors.getAllDescriptor());

        // Resolve an interceptor type
        return descriptors;
    }

    /**
     * Resolve the interceptor type ({@link InterceptorType}) for each interceptor declared in each plugin
     */
    private void resolveInterceptorType(Collection<Descriptors.Descriptor> descriptors) {
        LOG.info("Resolving interceptor type from all enabled plugins...");
        InterceptorTypeResolver resolver = new InterceptorTypeResolver(PluginClassLoaderManager.getDefaultLoader());

        for (Descriptors.Descriptor descriptor : descriptors) {
            for (Descriptors.MethodPointCuts pointcut : descriptor.getMethodPointCuts()) {
                for (MethodPointCutDescriptor pointcutDescriptor : pointcut.getMethodInterceptors()) {
                    try {
                        InterceptorType type = resolver.resolve(pointcutDescriptor.getInterceptorClassName());
                        pointcutDescriptor.setInterceptorType(type);
                    } catch (AgentException e) {
                        LOG.error("Unable to resolve interceptor type for [{}]. Exception: {}",
                                  pointcutDescriptor.getInterceptorClassName(),
                                  e.getMessage());
                    }
                }
            }
        }
        LOG.info("Resolving interceptor type completes.");
    }

    private List<IPlugin> loadPlugins() {
        JarClassLoader pluginClassLoader = PluginClassLoaderManager.getDefaultLoader();
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

            LOG.info("Found plugin [{}]", pluginName);
            return (IPlugin) pluginClass.getDeclaredConstructor().newInstance();
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

    protected abstract boolean onResolved(Class<?> pluginClazz);
}
