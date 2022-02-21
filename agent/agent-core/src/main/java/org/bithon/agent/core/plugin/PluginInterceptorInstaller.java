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

package org.bithon.agent.core.plugin;

import org.bithon.agent.bootstrap.aop.IBithonObject;
import org.bithon.agent.bootstrap.loader.JarClassLoader;
import org.bithon.agent.bootstrap.loader.PluginClassLoaderManager;
import org.bithon.agent.core.aop.descriptor.Descriptors;
import org.bithon.agent.core.aop.interceptor.InterceptorInstaller;
import org.bithon.agent.core.context.AgentContext;
import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.LoggerFactory;
import shaded.net.bytebuddy.agent.builder.AgentBuilder;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/18-20:36
 */
public class PluginInterceptorInstaller {

    private static final ILogAdaptor log = LoggerFactory.getLogger(PluginInterceptorInstaller.class);

    public static void install(AgentContext agentContext, Instrumentation inst) {
        // create plugin class loader first
        PluginClassLoaderManager.createDefault(agentContext.getAgentDirectory());

        // find all plugins first
        List<IPlugin> plugins = loadPlugins();

        // install interceptors
        Descriptors descriptors = mergeInterceptorDescriptors(plugins);

        new InterceptorInstaller(descriptors).installOn(createAgentBuilder(inst), inst);

        // start plugins
        plugins.forEach(IPlugin::start);

        // install shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> plugins.forEach(IPlugin::stop)));
    }

    private static AgentBuilder createAgentBuilder(Instrumentation inst) {
        AgentBuilder builder = new AgentBuilder.Default();

        builder = builder.assureReadEdgeFromAndTo(inst, IBithonObject.class);

        return builder;
    }

    private static Descriptors mergeInterceptorDescriptors(List<IPlugin> plugins) {
        Descriptors descriptors = new Descriptors();
        for (IPlugin plugin : plugins) {
            String pluginName = plugin.getClass().getSimpleName();

            descriptors.merge(plugin.getBithonClassDescriptor());

            descriptors.merge(pluginName, plugin.getPreconditions(), plugin.getInterceptors());
        }
        return descriptors;
    }

    private static List<IPlugin> loadPlugins() {

        JarClassLoader pluginClassLoader = PluginClassLoaderManager.getDefaultLoader();
        List<JarFile> pluginJars = pluginClassLoader.getJars()
                                                    .stream()
                                                    .sorted(Comparator.comparing(ZipFile::getName))
                                                    .collect(Collectors.toList());

        final List<IPlugin> plugins = new ArrayList<>();
        for (JarFile jar : pluginJars) {
            String jarFileName = new File(jar.getName()).getName();
            try {
                String pluginClassName = jar.getManifest().getMainAttributes().getValue("Plugin-Class");
                if (pluginClassName == null) {
                    log.info("Invalid plugin {}", jarFileName);
                    continue;
                }

                //noinspection unchecked
                Class<? extends IPlugin> pluginClass = (Class<? extends IPlugin>) Class.forName(pluginClassName,
                                                                                                true,
                                                                                                pluginClassLoader);

                Boolean isPluginDisabled = PluginConfigurationManager.load(pluginClass)
                                                                     .getConfig(PluginConfigurationManager.getPluginConfigurationPrefixName(pluginClassName)
                                                                                + "disabled", Boolean.class);
                if (isPluginDisabled != null && isPluginDisabled) {
                    log.info("Found plugin {}, but it's DISABLED by configuration", jarFileName);
                    continue;
                }

                log.info("Found plugin {}", jarFileName);
                plugins.add(pluginClass.getDeclaredConstructor().newInstance());
            } catch (Throwable e) {
                LoggerFactory.getLogger(PluginInterceptorInstaller.class)
                             .error(String.format(Locale.ENGLISH,
                                                  "Failed to add plugin from jar %s",
                                                  new File(jar.getName()).getName()),
                                    e);
            }
        }
        return plugins;
    }
}
