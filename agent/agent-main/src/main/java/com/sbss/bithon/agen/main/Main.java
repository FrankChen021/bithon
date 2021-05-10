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

package com.sbss.bithon.agen.main;

import com.sbss.bithon.agent.bootstrap.loader.AgentClassLoader;

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
    public static void premain(String agentArgs, Instrumentation inst) throws Exception {
        //
        // check whether to start agent
        //
        String disabled = System.getProperty("bithon.disabled", "false");
        if ("".equals(disabled) || "true".equals(disabled)) {
            System.out.println("bithon is disabled for the sake of -Dbithon.disabled");
            return;
        }

        //
        // check if agent is deployed correctly
        //
        boolean hasBootstrapJar = Arrays.stream(ManagementFactory.getRuntimeMXBean()
                                                                 .getBootClassPath()
                                                                 .split(File.pathSeparator))
                                        .anyMatch(path -> "agent-bootstrap.jar".equals(new File(path).getName()));
        if (!hasBootstrapJar) {
            throw new IllegalStateException("agent-bootstrap.jar is not on boot class path");
        }

        showBanner();

        initAppLogger();

        File agentDirectory = new BootstrapJarLocator().locate(Main.class.getName()).getParentFile();

        ClassLoader classLoader = AgentClassLoader.initialize(agentDirectory);
        Class<?> starterClass = classLoader.loadClass("com.sbss.bithon.agent.core.starter.AgentStarter");
        Object starterObject = starterClass.newInstance();
        Method startMethod = starterClass.getDeclaredMethod("start",
                                                            String.class,
                                                            Instrumentation.class);
        startMethod.invoke(starterObject, agentDirectory.getAbsolutePath(), inst);
    }

    /**
     * see {@link https://issues.apache.org/jira/browse/LOG4J2-1094}
     */
    private static void initAppLogger() {
        try {
            Class<?> loggerFactoryClass = Class.forName("org.slf4j.LoggerFactory",
                                                        true,
                                                        ClassLoader.getSystemClassLoader());
            Method getLoggerMethod = loggerFactoryClass.getDeclaredMethod("getLogger", String.class);
            getLoggerMethod.invoke(null, "none");
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
        }
    }

    /**
     * The banner is generated on https://manytools.org/hacker-tools/ascii-banner/ with font = 3D-ASCII
     */
    private static void showBanner() {
        System.out.println(" ________  ___  _________  ___  ___  ________  ________      \n"
                           + "|\\   __  \\|\\  \\|\\___   ___\\\\  \\|\\  \\|\\   __  \\|\\   ___  \\    \n"
                           + "\\ \\  \\|\\ /\\ \\  \\|___ \\  \\_\\ \\  \\\\\\  \\ \\  \\|\\  \\ \\  \\\\ \\  \\   \n"
                           + " \\ \\   __  \\ \\  \\   \\ \\  \\ \\ \\   __  \\ \\  \\\\\\  \\ \\  \\\\ \\  \\  \n"
                           + "  \\ \\  \\|\\  \\ \\  \\   \\ \\  \\ \\ \\  \\ \\  \\ \\  \\\\\\  \\ \\  \\\\ \\  \\ \n"
                           + "   \\ \\_______\\ \\__\\   \\ \\__\\ \\ \\__\\ \\__\\ \\_______\\ \\__\\\\ \\__\\\n"
                           + "    \\|_______|\\|__|    \\|__|  \\|__|\\|__|\\|_______|\\|__| \\|__|\n"
                           + "                                                             ");
    }
}
