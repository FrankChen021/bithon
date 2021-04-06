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
