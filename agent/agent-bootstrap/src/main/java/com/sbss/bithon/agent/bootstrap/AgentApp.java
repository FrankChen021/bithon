package com.sbss.bithon.agent.bootstrap;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author frankchen
 */
public class AgentApp {
    public static void premain(String agentArgs,
                               Instrumentation inst) throws Exception {

        String agentPath = new File("/Users/frankchen/Documents/source/eclipse-workspace-frank/bithon/agent/dest/agent-bootstrap.jar").getParentFile().getPath();

        List<File> libs = resolveJars(agentPath + "/lib");
        libs.addAll(resolveJars(agentPath+"/plugins"));
        ClassLoader classLoader = AgentClassLoader.createInstance(libs, AgentApp.class.getClassLoader());
        Class<?> agentStarterClass = classLoader.loadClass("com.sbss.bithon.agent.core.plugin.AgentStarter");
        Object starter = agentStarterClass.newInstance();
        Method start = agentStarterClass.getDeclaredMethod("start", String.class, ClassLoader.class, Instrumentation.class);
        start.invoke(starter, agentPath, classLoader, inst);
    }

    private static List<File> resolveJars(String dir) {
        return Arrays.stream(new File(dir).list((directory, name) -> name.endsWith(".jar"))).map(jar -> {
            return new File(dir + '/' + jar);
        }).collect(Collectors.toList());
    }
}
