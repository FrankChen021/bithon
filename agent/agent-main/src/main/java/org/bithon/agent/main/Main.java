/*
 *    Copyright 2020 bithon.org
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

package org.bithon.agent.main;

import org.bithon.agent.instrumentation.loader.AgentClassLoader;
import org.bithon.agent.instrumentation.utils.AgentDirectory;
import org.bithon.agent.instrumentation.utils.JarLocator;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * @author frankchen
 */
public class Main {
    public static void premain(String agentArgs, Instrumentation inst) throws Throwable {
        if (AgentDirectory.getRoot() != null) {
            System.err.println("Re-initialization of agent is skipped. This might be caused by multiple agent jars in the classpath. Please check your java command line.");
            return;
        }

        if (!shouldEnableAgent()) {
            return;
        }

        //
        // check whether to start agent
        //
        String disabled = System.getProperty("bithon.disabled", "false");
        if ("".equals(disabled) || "true".equalsIgnoreCase(disabled)) {
            System.out.println("bithon is disabled for the sake of -Dbithon.disabled");
            return;
        }

        //
        // agent-instrumentation.jar should be on the boot-class-path
        // check if agent is deployed correctly
        //
        if (ManagementFactory.getRuntimeMXBean().isBootClassPathSupported()) {
            boolean hasBootstrapJar = Arrays.stream(ManagementFactory.getRuntimeMXBean()
                                                                     .getBootClassPath()
                                                                     .split(File.pathSeparator))
                                            .anyMatch(path -> path.endsWith(File.separator + "agent-instrumentation.jar"));
            if (!hasBootstrapJar) {
                throw new IllegalStateException("agent-instrumentation.jar is not on boot class path");
            }
        }

        // agent-main.jar is located under agent directory,
        // So its parent is the right directory of the agent
        AgentDirectory.setRoot(JarLocator.locate(Main.class.getName()).getParentFile());

        Class<?> starterClass = AgentClassLoader.getClassLoader().loadClass("org.bithon.agent.starter.AgentStarter");
        Object starterObject = starterClass.getDeclaredConstructor().newInstance();
        Method startMethod = starterClass.getDeclaredMethod("start",
                                                            Instrumentation.class);
        try {
            startMethod.invoke(starterObject, inst);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    /**
     * When the agent is set in the global JAVA_TOOL_OPTIONS environment variable,
     * all Java commands (including commands like 'java -version')
     * will launch the agent which might not be expected.
     * <p>
     * In such a case, the agent is not able to exit correctly which hangs the java command.
     * <p>
     * So here we check if the java command is going to launch a java application, if NOT, we will exit the agent silently.
     */
    private static boolean shouldEnableAgent() {
        String toolOptions = System.getenv("JAVA_TOOL_OPTIONS");
        if (toolOptions == null) {
            // JAVA_TOOL_OPTIONS is not set, the agent should be enabled at this stage.
            return true;
        }

        if (ManagementFactory.getRuntimeMXBean().getSystemProperties().get("sun.java.command") == null) {
            System.out.println("The agent is disabled because the java command is not going to launch a java application.");
            return false;
        }
        return true;
    }
}
