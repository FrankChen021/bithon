package com.sbss.bithon.agent.bootstrap;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;

/**
 * @author frankchen
 */
public class AgentApp {
    public static void premain(String agentArgs,
                               Instrumentation inst) throws Exception {

        String agentPath = new File(AgentApp.class.getProtectionDomain()
                                                  .getCodeSource()
                                                  .getLocation()
                                                  .getFile()).getParentFile().getPath();

        ClassLoader classLoader = new AgentClassLoader(agentPath + "/lib");
        Class<?> agentStarterClass = classLoader.loadClass("com.sbss.bithon.agent.core.plugin.AgentStarter");
        Object starter = agentStarterClass.newInstance();
        Method start = agentStarterClass.getDeclaredMethod("start", String.class, ClassLoader.class, Instrumentation.class);
        start.invoke(starter, agentPath, classLoader, inst);
    }
}
