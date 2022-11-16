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

package org.bithon.agent.plugin.kafka.shared;

/**
 *
 * @author frankchen
 */
public class Tools {
    /**
     * Turn a name in camel style into - separated style
     * Used to match names in metrics
     */
    public static String camelCaseToDash(String name) {
        StringBuilder value = new StringBuilder(name.length());
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            value.append(c);
            if (i + 1 < name.length()) {
                char n = name.charAt(i + 1);
                if (Character.isUpperCase(n)) {
                    value.append('-');
                    n = (char) (n - 'A' + 'a');
                    value.append(n);
                    i++;
                }
            }
        }
        return value.toString();
    }
}
