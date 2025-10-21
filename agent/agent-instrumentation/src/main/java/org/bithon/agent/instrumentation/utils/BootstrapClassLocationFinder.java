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

package org.bithon.agent.instrumentation.utils;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility to find the location of classes loaded by the bootstrap classloader.
 * Bootstrap classloader classes (loaded via Boot-Class-Path) don't have CodeSource set,
 * so we need to manually track them.
 *
 * <p>In JDK 9+, the boot classpath concept changed and ManagementFactory.getRuntimeMXBean().getBootClassPath()
 * is no longer supported. Instead, we get the location of these jars if they're provided in the "boot" subdirectory
 *
 * @author frank.chen021@outlook.com
 * @date 2025/10/15
 */
public class BootstrapClassLocationFinder {

    private static final Map<String, URL> BOOTSTRAP_JAR_LOCATIONS = new HashMap<>();

    static {
        File bootstrapDirectory = AgentDirectory.getSubDirectory("boot");
        if (bootstrapDirectory.exists() && bootstrapDirectory.isDirectory()) {
            File[] jarFiles = bootstrapDirectory.listFiles((dir, name) -> name.endsWith(".jar"));
            if (jarFiles != null) {
                for (File jarFile : jarFiles) {
                    try {
                        BOOTSTRAP_JAR_LOCATIONS.put(jarFile.getName(), jarFile.toURI().toURL());
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }

    /**
     * Try to find the location of a class, even if it's loaded by the bootstrap classloader.
     *
     * @param clazz the class to find location for
     * @return the URL of the JAR file containing the class, or null if not found
     */
    public static URL findLocation(Class<?> clazz) {
        String className = clazz.getName();
        if (className.startsWith("org.bithon.agent.")) {
            return BOOTSTRAP_JAR_LOCATIONS.get("agent-instrumentation.jar");
        } else if (className.startsWith("org.bithon.shaded.net.bytebuddy.")) {
            return BOOTSTRAP_JAR_LOCATIONS.get("shaded-bytebuddy.jar");
        } else {
            return null;
        }
    }
}

