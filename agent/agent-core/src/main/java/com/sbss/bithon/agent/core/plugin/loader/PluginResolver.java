package com.sbss.bithon.agent.core.plugin.loader;

import com.sbss.bithon.agent.dependency.AgentDependencyManager;
import com.sbss.bithon.agent.core.plugin.AbstractPlugin;
import com.sbss.bithon.agent.dependency.JarFileItem;
import shaded.org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/20 12:05 上午
 */
public class PluginResolver {

    public List<AbstractPlugin> resolve() {

        final List<AbstractPlugin> plugins = new ArrayList<>();
        for (JarFileItem jar : AgentDependencyManager.getPlugins()) {
            try {
                String pluginClassName = jar.getJarFile().getManifest().getMainAttributes().getValue("Plugin-Class");
                AbstractPlugin plugin = (AbstractPlugin) Class.forName(pluginClassName,
                                                                       true,
                                                                       AgentDependencyManager.getClassLoader())
                                                              .newInstance();
                plugins.add(plugin);
            } catch (Throwable e) {
                LoggerFactory.getLogger(PluginResolver.class)
                             .error(String.format("Failed to add plugin from jar %s", jar.getSourceFile().getName()),
                                    e);
            }
        }
        return plugins;
    }
}
