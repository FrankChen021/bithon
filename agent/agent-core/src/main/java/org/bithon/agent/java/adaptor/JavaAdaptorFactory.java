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

package org.bithon.agent.java.adaptor;

import org.bithon.agent.instrumentation.expt.AgentException;
import org.bithon.agent.instrumentation.utils.AgentDirectory;
import org.bithon.agent.instrumentation.utils.JdkUtils;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 2025/8/31 14:03
 */
public class JavaAdaptorFactory {
    private static IJavaAdaptor ADAPTOR;

    public static IJavaAdaptor create(Instrumentation instrumentation) {
        //
        // Find the right adaptor jar for the current running JRE version
        //
        File jarDirectory = AgentDirectory.getSubDirectory("adaptor");
        if (!jarDirectory.exists()) {
            throw new AgentException("The adaptor directory: " + jarDirectory.getAbsolutePath() + ". Please report to agent maintainers.");
        }
        int javaVersion = JdkUtils.CURRENT_JAVA_VERSION;
        for (int detectJavaVersion = javaVersion; detectJavaVersion >= 8; detectJavaVersion--) {
            File adaptorJar = new File(jarDirectory, String.format(Locale.ENGLISH, "agent-java-adaptor-java%d.jar", detectJavaVersion));
            if (!adaptorJar.exists()) {
                continue;
            }

            try {
                try (URLClassLoader classLoader = new URLClassLoader(new URL[]{adaptorJar.toURI().toURL()},
                                                                     JavaAdaptorFactory.class.getClassLoader())) {
                    Class<?> adaptorClass = classLoader.loadClass(String.format(Locale.ENGLISH, "org.bithon.agent.java.adaptor.Java%dAdaptor",
                                                                                detectJavaVersion));
                    ADAPTOR = (IJavaAdaptor) adaptorClass.getConstructor(Instrumentation.class)
                                                         .newInstance(instrumentation);
                    return ADAPTOR;
                }
            } catch (Throwable e) {
                throw new AgentException("Failed to load java adaptor from " + adaptorJar.getAbsolutePath(), e);
            }
        }

        File[] jars = jarDirectory.listFiles((dir, name) -> name.startsWith("agent-java-adaptor-java") && name.endsWith(".jar"));
        String availableJars = jars == null ? "Unknown" : Arrays.stream(jars)
                                                                .map(File::getName)
                                                                .collect(Collectors.joining(","));
        throw new AgentException("Cannot find a suitable java adaptor for current java version [%d]. Available adaptors are [%s]",
                                 javaVersion,
                                 availableJars);
    }

    public static IJavaAdaptor getAdaptor() {
        if (ADAPTOR != null) {
            return ADAPTOR;
        }

        throw new AgentException("JavaAdaptorFactory is not created. Please report to agent maintainers.");
    }

    /**
     * ONLY for test purpose
     */
    public static void setAdaptor(IJavaAdaptor adaptor) {
        ADAPTOR = adaptor;
    }
}
