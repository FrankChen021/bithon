package com.sbss.bithon.agent.bootstrap;

import com.sbss.bithon.agent.core.plugin.aop.AgentClassLoader;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;

/**
 * @author frankchen
 */
public class AgentApp {
    public static void premain(String agentArgs,
                               Instrumentation inst) throws Exception {

        File agentDirectory = new BootstrapJarLocator().locate(AgentApp.class.getName()).getParentFile();

        ClassLoader classLoader = AgentClassLoader.createInstance(agentDirectory);
        Class<?> agentStarterClass = classLoader.loadClass("com.sbss.bithon.agent.core.plugin.AgentStarter");
        Object starter = agentStarterClass.newInstance();
        Method start = agentStarterClass.getDeclaredMethod("start",
                                                           String.class,
                                                           ClassLoader.class,
                                                           Instrumentation.class);
        start.invoke(starter, agentDirectory.getAbsolutePath(), classLoader, inst);
    }
}
