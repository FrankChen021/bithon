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

package com.sbss.bithon.agent.core.plugin.interceptor;

import com.sbss.bithon.agent.bootstrap.loader.JarClassLoader;
import com.sbss.bithon.agent.core.context.AgentContext;
import com.sbss.bithon.agent.core.plugin.AbstractPlugin;
import com.sbss.bithon.agent.core.plugin.InstrumentationHelper;
import com.sbss.bithon.agent.core.plugin.PluginClassLoaderManager;
import shaded.net.bytebuddy.agent.builder.AgentBuilder;
import shaded.org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.jar.JarFile;
import java.util.zip.ZipFile;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/18-20:36
 */
public class PluginInstaller {

    public static void install(AgentContext agentContext, Instrumentation inst) {
        // find all plugins first
        List<AbstractPlugin> plugins = resolvePlugins();

        // install interceptors for bootstrap classes
        AgentBuilder agentBuilder = new PluginAopGenerator(inst,
                                                           new AgentBuilder.Default()).generate(plugins);

        // install interceptors
        new PluginInterceptorInstaller(agentBuilder, inst).install(plugins);

        // start plugins
        plugins.forEach((plugin) -> plugin.start());

        // install shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> plugins.forEach((plugin) -> plugin.stop())));

        InstrumentationHelper.setInstance(inst);
    }

    public static List<AbstractPlugin> resolvePlugins() {

        JarClassLoader pluginClassLoader = PluginClassLoaderManager.getDefaultLoader();
        List<JarFile> pluginJars = new ArrayList<>(pluginClassLoader.getJars());
        pluginJars.sort(Comparator.comparing(ZipFile::getName));

        final List<AbstractPlugin> plugins = new ArrayList<>();
        for (JarFile jar : pluginJars) {
            try {
                LoggerFactory.getLogger(PluginInstaller.class)
                             .info("Found {}", new File(jar.getName()).getName());

                String pluginClassName = jar.getManifest().getMainAttributes().getValue("Plugin-Class");
                if (pluginClassName == null) {
                    continue;
                }

                AbstractPlugin plugin = (AbstractPlugin) Class.forName(pluginClassName,
                                                                       true,
                                                                       pluginClassLoader)
                                                              .newInstance();
                plugins.add(plugin);
            } catch (Throwable e) {
                LoggerFactory.getLogger(PluginInstaller.class)
                             .error(String.format("Failed to add plugin from jar %s",
                                                  new File(jar.getName()).getName()),
                                    e);
            }
        }
        return plugins;
    }
}
