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

package org.bithon.agent.observability.metric.collector.jvm;

/**
 * Factory class for creating the appropriate MaxDirectMemoryCollector implementation
 * based on the current JDK version. This factory uses runtime detection to choose
 * between JDK 8 and JDK 9+ implementations without using reflection.
 *
 * @author frank.chen021@outlook.com
 * @date 2024/12/19
 */
class MaxDirectMemoryCollectorFactory {

    /**
     * Creates the appropriate MaxDirectMemoryCollector implementation for the current JDK version.
     * This method checks the runtime JDK version to decide which implementation to use:
     * - JDK 8: Uses MaxDirectMemoryCollectorJdk8 (reflection-based sun.misc.VM)
     * - JDK 9+: Uses MaxDirectMemoryCollectorJdk9 (direct access to jdk.internal.misc.VM)
     *
     * Uses reflection to load implementation classes to avoid class loading issues when
     * running on different JDK versions.
     *
     * @return the appropriate collector implementation
     * @throws Exception if the implementation cannot be created
     */
    static IMaxDirectMemoryCollector create() throws Exception {
        int javaVersion = getJavaVersion();
        
        if (javaVersion >= 9) {
            // JDK 9+ - use direct access to jdk.internal.misc.VM
            try {
                Class<?> jdk9Class = Class.forName("org.bithon.agent.observability.metric.collector.jvm.MaxDirectMemoryCollectorJdk9");
                return (IMaxDirectMemoryCollector) jdk9Class.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new Exception("JDK " + javaVersion + " detected but MaxDirectMemoryCollectorJdk9 cannot be instantiated: " + e.getMessage(), e);
            }
        } else {
            // JDK 8 - use reflection-based access to sun.misc.VM
            try {
                Class<?> jdk8Class = Class.forName("org.bithon.agent.observability.metric.collector.jvm.MaxDirectMemoryCollectorJdk8");
                return (IMaxDirectMemoryCollector) jdk8Class.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new Exception("JDK " + javaVersion + " detected but MaxDirectMemoryCollectorJdk8 cannot be instantiated: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Gets the major version of the current Java runtime.
     * 
     * @return the Java major version (e.g., 8, 9, 11, 17, 21)
     */
    private static int getJavaVersion() {
        String version = System.getProperty("java.version");
        
        // Handle different version formats:
        // JDK 8: "1.8.0_XXX" 
        // JDK 9+: "9.0.X", "11.0.X", "17.0.X", etc.
        if (version.startsWith("1.")) {
            // JDK 8 format: "1.8.0_XXX"
            return Integer.parseInt(version.substring(2, 3));
        } else {
            // JDK 9+ format: "9.0.X", "11.0.X", etc.
            int dotIndex = version.indexOf('.');
            if (dotIndex > 0) {
                return Integer.parseInt(version.substring(0, dotIndex));
            } else {
                // Fallback: try to parse the first part
                return Integer.parseInt(version.split("[^0-9]")[0]);
            }
        }
    }
}

