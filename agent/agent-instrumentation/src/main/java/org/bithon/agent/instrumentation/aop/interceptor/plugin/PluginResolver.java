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

import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.Enumeration;
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
        List<IPlugin> plugins = resolvePlugins();

        Descriptors descriptors = new Descriptors();
        for (IPlugin plugin : plugins) {
            String pluginName = plugin.getClass().getSimpleName();

            descriptors.merge(plugin.getBithonClassDescriptor());

            descriptors.merge(pluginName, plugin.getPreconditions(), plugin.getInterceptors());
        }

        resolveInterceptorType(descriptors.getAllDescriptor());

        // Resolve interceptor type
        return descriptors;
    }

    public void resolveInterceptorsFromAnnotation() {
        for (JarFile jarFile : PluginClassLoaderManager.getDefaultLoader()
                                                       .getJars()) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry jarEntry = entries.nextElement();
                try {
                    jarFile.getInputStream(jarEntry);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private void resolveInterceptorType(Collection<Descriptors.Descriptor> descriptors) {
        LOG.info("Resolving interceptors...");
        InterceptorTypeResolver resolver = new InterceptorTypeResolver(PluginClassLoaderManager.getDefaultLoader());

        for (Descriptors.Descriptor descriptor : descriptors) {
            for (Descriptors.MethodPointCuts pointcut : descriptor.getMethodPointCuts()) {
                for (MethodPointCutDescriptor pointcutDescriptor : pointcut.getMethodInterceptors()) {
                    try {
                        InterceptorType type = resolver.resolve(pointcutDescriptor.getInterceptorClassName());
                        pointcutDescriptor.setInterceptorType(type);
                    } catch (AgentException e) {
                        LOG.error("Unable to resolve interceptor type for [%s]", pointcutDescriptor.getInterceptorClassName());
                    }
                }
            }
        }

        LOG.info("Resolving interceptors Completes.");
    }

    private List<IPlugin> resolvePlugins() {
        JarClassLoader pluginClassLoader = PluginClassLoaderManager.getDefaultLoader();
        return pluginClassLoader.getJars()
                                .stream()
                                .flatMap(JarFile::stream)
                                .filter(jarEntry -> jarEntry.getName().endsWith("Plugin.class"))
                                .sorted(Comparator.comparing(JarEntry::getName))
                                .map((jarEntry) -> resolve(jarEntry, pluginClassLoader))
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList());
    }

    private IPlugin resolve(JarEntry jarEntry, JarClassLoader pluginClassLoader) {
        String jarEntryName = jarEntry.getName();
        String pluginFullClassName = jarEntryName.substring(0, jarEntryName.length() - ".class".length()).replace('/', '.');

        try {
            Class<?> pluginClass = Class.forName(pluginFullClassName, true, pluginClassLoader);
            if (!isPluginClass(pluginClass)) {
                LOG.info("Resource [{}] is not type of IPlugin. The class name does not comply with the plugin standard. Please change it.",
                         pluginFullClassName);
                return null;
            }

            if (!resolve(pluginClass)) {
                LOG.info("Found plugin {}, but it's not DISABLED by configuration", pluginClass.getSimpleName());
                return null;
            }

            LOG.info("Found plugin {}", pluginClass.getSimpleName());
            return (IPlugin) pluginClass.getDeclaredConstructor().newInstance();
        } catch (Throwable e) {
            LOG.error(String.format(Locale.ENGLISH, "Failed to load plugin [%s]", pluginFullClassName), e);
            return null;
        }
    }

    private boolean isPluginClass(Class<?> possiblePluginClass) {
        for (Class<?> inf : possiblePluginClass.getInterfaces()) {
            if (inf.equals(IPlugin.class)) {
                return true;
            }
        }
        Class<?> parentClass = possiblePluginClass.getSuperclass();
        return parentClass != null && isPluginClass(parentClass);
    }

    protected abstract boolean resolve(Class<?> pluginClazz);
}
