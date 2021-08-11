/*
 *    Copyright 2020 bithon.cn
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

package com.sbss.bithon.agent.core.plugin;

import com.sbss.bithon.agent.bootstrap.loader.JarClassLoader;
import com.sbss.bithon.agent.core.aop.AopClassGenerator;
import com.sbss.bithon.agent.core.aop.descriptor.InterceptorDescriptor;
import com.sbss.bithon.agent.core.aop.interceptor.InterceptorInstaller;
import com.sbss.bithon.agent.core.config.AgentConfigManager;
import com.sbss.bithon.agent.core.context.AgentContext;
import shaded.net.bytebuddy.agent.builder.AgentBuilder;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/18-20:36
 */
public class PluginInterceptorInstaller {

    private static final Logger log = LoggerFactory.getLogger(PluginInterceptorInstaller.class);

    public static void install(AgentContext agentContext, Instrumentation inst) {
        // create plugin class loader first
        PluginClassLoaderManager.createDefault(agentContext.getAgentDirectory());

        // find all plugins first
        List<IPlugin> plugins = loadPlugins();

        // install interceptors for bootstrap classes
        AgentBuilder agentBuilder = new AopClassGenerator(inst,
                                                          new AgentBuilder.Default()).generate(plugins);

        // install interceptors
        install(agentBuilder, inst, plugins);

        // start plugins
        plugins.forEach(IPlugin::start);

        // install shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> plugins.forEach(IPlugin::stop)));
    }

    private static void install(AgentBuilder agentBuilder, Instrumentation inst, List<IPlugin> plugins) {
        for (IPlugin plugin : plugins) {
            // this installer must be instantiated for each plugin
            InterceptorInstaller installer = new InterceptorInstaller(agentBuilder, inst);
            installer.transformToBithonClass(plugin.getBithonClassDescriptor());

            for (InterceptorDescriptor interceptor : plugin.getInterceptors()) {
                installer.installInterceptor(plugin.getClass().getSimpleName(), interceptor, plugin.getPreconditions());
            }
        }
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
                    log.info("Found plugin {}", jarFileName);
                    continue;
                }

                String disabledConfigName = "agent.plugin." + getPluginPackageName(pluginClassName) + ".disabled";
                Boolean pluginDisabled = AgentConfigManager.getInstance()
                                                           .getConfig(disabledConfigName, Boolean.class);
                if (pluginDisabled != null && pluginDisabled) {
                    log.info("Found plugin {}, but it's DISABLED by configuration", jarFileName);
                    continue;
                }

                log.info("Found plugin {}", jarFileName);

                IPlugin plugin = (IPlugin) Class.forName(pluginClassName,
                                                         true,
                                                         pluginClassLoader)
                                                .newInstance();
                plugins.add(plugin);
            } catch (Throwable e) {
                LoggerFactory.getLogger(PluginInterceptorInstaller.class)
                             .error(String.format("Failed to add plugin from jar %s",
                                                  new File(jar.getName()).getName()),
                                    e);
            }
        }
        return plugins;
    }

    private static String getPluginPackageName(String pluginClassName) {
        String[] parts = pluginClassName.split("\\.");
        if (parts.length > 1) {
            return parts[parts.length - 2];
        }
        return null;
    }
}
