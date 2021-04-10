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
