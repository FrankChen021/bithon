package com.sbss.bithon.agent.core.plugin.loader;

import com.sbss.bithon.agent.core.context.AgentContext;
import com.sbss.bithon.agent.core.plugin.AbstractPlugin;
import shaded.net.bytebuddy.agent.builder.AgentBuilder;

import java.lang.instrument.Instrumentation;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/18-20:36
 */
public class PluginInstaller {

    public static void install(AgentContext agentContext, Instrumentation inst) {
        // find all plugins first
        List<AbstractPlugin> plugins = new PluginResolver().resolve();

        // install interceptors for bootstrap classes
        AgentBuilder agentBuilder = new BootstrapAopGenerator(inst,
                                                              new AgentBuilder.Default()).generate(plugins);

        // install interceptors
        new PluginInterceptorInstaller(agentBuilder, inst).install(plugins);

        // start plugins
        plugins.forEach((plugin) -> plugin.start());

        // install shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> plugins.forEach((plugin) -> plugin.stop())));
    }
}
