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

package org.bithon.agent.observability.utils;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/21 22:28
 */
public class MiscUtils {

    /**
     * clean up parameters on connection string
     * <p>
     * We don't parse DB,HostAndPort from connection string at agent side
     * because the rules are a little bit complex which would cause more frequent upgrading of agent
     */
    public static String cleanupConnectionString(String connectionString) {
        if (connectionString == null || connectionString.isEmpty()) {
            return connectionString;
        }
        
        // Vectorized search for '?' or ';' - process 4 characters at a time
        int len = connectionString.length();
        int i = 0;
        
        // Process 4 characters at a time for vectorization
        for (; i < len - 3; i += 4) {
            char c1 = connectionString.charAt(i);
            char c2 = connectionString.charAt(i + 1);
            char c3 = connectionString.charAt(i + 2);
            char c4 = connectionString.charAt(i + 3);
            
            if (c1 == '?' || c1 == ';') {
                return connectionString.substring(0, i);
            }
            if (c2 == '?' || c2 == ';') {
                return connectionString.substring(0, i + 1);
            }
            if (c3 == '?' || c3 == ';') {
                return connectionString.substring(0, i + 2);
            }
            if (c4 == '?' || c4 == ';') {
                return connectionString.substring(0, i + 3);
            }
        }
        
        // Handle remaining characters (less than 4)
        for (; i < len; i++) {
            char c = connectionString.charAt(i);
            if (c == '?' || c == ';') {
                return connectionString.substring(0, i);
            }
        }
        
        // If neither character is found, return the original string
        return connectionString;
    }
}
