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

package com.sbss.bithon.agent.core.plugin.loader;

import com.sbss.bithon.agent.boot.loader.AgentDependencyManager;
import com.sbss.bithon.agent.core.plugin.AbstractPlugin;
import shaded.org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/20 12:05 上午
 */
public class PluginResolver {

    public List<AbstractPlugin> resolve() {

        final List<AbstractPlugin> plugins = new ArrayList<>();
        for (JarFile jar : AgentDependencyManager.getPlugins()) {
            try {
                String pluginClassName = jar.getManifest().getMainAttributes().getValue("Plugin-Class");
                AbstractPlugin plugin = (AbstractPlugin) Class.forName(pluginClassName,
                                                                       true,
                                                                       AgentDependencyManager.getClassLoader())
                                                              .newInstance();
                plugins.add(plugin);
            } catch (Throwable e) {
                LoggerFactory.getLogger(PluginResolver.class)
                             .error(String.format("Failed to add plugin from jar %s",
                                                  new File(jar.getName()).getName()),
                                    e);
            }
        }
        return plugins;
    }
}
