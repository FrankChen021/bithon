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

import java.lang.management.ManagementFactory;

public class JdkUtils {
    public static final int CURRENT_JAVA_VERSION;
    
    static {
        CURRENT_JAVA_VERSION = getCurrentJavaVersion();
    }

    private static int getCurrentJavaVersion() {
        String specVersion = ManagementFactory.getRuntimeMXBean().getSpecVersion();
        String[] versionParts = specVersion.split("\\.");
        if (versionParts[0].equals("1")) {
            // For Java 1.x (e.g., 1.8)
            return Integer.parseInt(versionParts[1]);
        } else {
            // For Java 9 and above
            return Integer.parseInt(versionParts[0]);
        }
    }
}
