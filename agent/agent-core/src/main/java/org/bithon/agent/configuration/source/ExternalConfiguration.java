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

import org.bithon.agent.configuration.Configuration;

import java.io.File;
import java.util.Properties;

/**
 * @author Frank Chen
 * @date 20/2/24 3:10 pm
 */
public class ExternalConfiguration {
    public static Configuration build() {
        // Get All arguments by bithon.configuration prefix
        Properties properties = CommandLineArgsConfiguration.parseCommandLineArgs("bithon.configuration.");
        if (properties.isEmpty()) {
            return null;
        }

        // Get the location property
        String configurationLocation = properties.getProperty("location");
        if (configurationLocation == null) {
            return null;
        }

        return Configuration.from(ConfigurationSource.EXTERNAL, new File(configurationLocation), true);
    }
}
