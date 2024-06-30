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

package org.bithon.agent.configuration.source;

import org.bithon.agent.instrumentation.expt.AgentException;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

/**
 * @author Frank Chen
 * @date 20/2/24 3:03 pm
 */
public class EnvironmentSource {

    public static PropertySource build(String envPrefix) {
        StringBuilder propertyText = new StringBuilder();

        for (Map.Entry<String, String> entry : Helper.getEnvironmentVariables().entrySet()) {
            String name = entry.getKey();
            String value = entry.getValue();
            if (name.startsWith(envPrefix) && !value.isEmpty()) {
                name = name.substring(envPrefix.length())
                           // For env, the underscore is used as a replacement of '.' character,
                           // Here we need to convert these characters back.
                           // Additionally, as the env variables are in uppercase, we need to convert them to lowercase.
                           .replace('_', '.').toLowerCase(Locale.ENGLISH);
                if (!name.isEmpty()) {
                    propertyText.append(name);
                    propertyText.append('=');
                    propertyText.append(value);
                    propertyText.append('\n');
                }
            }
        }

        if (propertyText.length() > 0) {
            try {
                return PropertySource.from(PropertySourceType.ENVIRONMENT_VARIABLES, "environment", propertyText.toString());
            } catch (IOException e) {
                throw new AgentException("Failed to read property user configuration:%s",
                                         e.getMessage());
            }
        } else {
            return null;
        }
    }
}
