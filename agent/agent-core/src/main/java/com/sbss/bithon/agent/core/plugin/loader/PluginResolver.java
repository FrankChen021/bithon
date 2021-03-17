package com.sbss.bithon.agent.core.plugin.loader;

import com.sbss.bithon.agent.core.context.AgentContext;
import com.sbss.bithon.agent.core.plugin.AbstractPlugin;
import shaded.org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static java.io.File.separator;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/20 12:05 上午
 */
public class PluginResolver {

    private final List<JarFileItem> jars;

    public PluginResolver(String agentDirectory) {
        jars = JarFileResolver.resolve(new File(agentDirectory + separator + AgentContext.PLUGIN_DIR));

        AgentClassloader.appendSearchFiles(jars);
    }

    public List<AbstractPlugin> resolve() {
        final List<AbstractPlugin> plugins = new ArrayList<>();
        for (JarFileItem jar : jars) {
            try {
                String pluginClassName = jar.getJarFile().getManifest().getMainAttributes().getValue("Plugin-Class");
                AbstractPlugin plugin = (AbstractPlugin) Class.forName(pluginClassName,
                                                                       true,
                                                                       AgentClassloader.getDefaultInstance())
                                                              .newInstance();
                plugins.add(plugin);
            } catch (Throwable e) {
                LoggerFactory.getLogger(PluginResolver.class)
                             .error(String.format("Failed to add plugin from jar %s", jar.sourceFile.getName()), e);
            }
        }
        return plugins;
    }
}
