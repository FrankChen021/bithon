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
import java.security.CodeSource;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility to find the location of classes loaded by the bootstrap classloader.
 * Bootstrap classloader classes (loaded via Boot-Class-Path) don't have CodeSource set,
 * so we need to manually track them.
 * 
 * <p>In JDK 9+, the boot classpath concept changed and ManagementFactory.getRuntimeMXBean().getBootClassPath()
 * is no longer supported. Instead, we manually register bootstrap JARs from the agent's premain method.
 *
 * @author frank.chen021@outlook.com
 * @date 2025/10/15
 */
public class BootstrapClassLocationFinder {
    
    private static final Map<String, URL> BOOTSTRAP_JAR_LOCATIONS = new HashMap<>();
    
    /**
     * Register a bootstrap JAR and its location.
     * This should be called during agent initialization for JARs specified in Boot-Class-Path.
     * 
     * @param jarName the name of the JAR file (e.g., "agent-instrumentation.jar")
     * @param jarFile the File object pointing to the JAR
     */
    public static void registerBootstrapJar(String jarName, File jarFile) {
        try {
            BOOTSTRAP_JAR_LOCATIONS.put(jarName, jarFile.toURI().toURL());
        } catch (Exception e) {
            // Log but don't fail - this is just for introspection
            System.err.println("Failed to register bootstrap jar " + jarName + ": " + e.getMessage());
        }
    }
    
    /**
     * Try to find the location of a class, even if it's loaded by the bootstrap classloader.
     * 
     * @param clazz the class to find location for
     * @return the URL of the JAR file containing the class, or null if not found
     */
    public static URL findLocation(Class<?> clazz) {
        // First try the normal way
        CodeSource codeSource = clazz.getProtectionDomain().getCodeSource();
        if (codeSource != null && codeSource.getLocation() != null) {
            return codeSource.getLocation();
        }
        
        // If the class is loaded by bootstrap classloader, try to find it in our map
        ClassLoader classLoader = clazz.getClassLoader();
        if (classLoader == null) {
            // null classloader means bootstrap classloader
            // Check if this is an agent class
            String className = clazz.getName();
            if (className.startsWith("org.bithon.agent.")) {
                return BOOTSTRAP_JAR_LOCATIONS.get("agent-instrumentation.jar");
            } else if (className.startsWith("org.bithon.shaded.net.bytebuddy.")) {
                return BOOTSTRAP_JAR_LOCATIONS.get("shaded-bytebuddy.jar");
            }
        }
        
        return null;
    }
    
    /**
     * Get all bootstrap JAR locations that were added via Boot-Class-Path
     */
    public static Map<String, URL> getBootstrapJarLocations() {
        return new HashMap<>(BOOTSTRAP_JAR_LOCATIONS);
    }
}

