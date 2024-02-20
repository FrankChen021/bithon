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
import org.bithon.agent.instrumentation.expt.AgentException;
import org.bithon.component.commons.utils.StringUtils;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * @author Frank Chen
 * @date 20/2/24 3:01 pm
 */
public class CommandLineArgsConfiguration {
    public static Configuration build(String commandLineArgPrefix) {
        Properties userPropertyMap = parseCommandLineArgs(commandLineArgPrefix);

        StringBuilder userProperties = new StringBuilder();
        for (Map.Entry<Object, Object> entry : userPropertyMap.entrySet()) {
            String name = (String) entry.getKey();
            String value = (String) entry.getValue();

            userProperties.append(name);
            userProperties.append('=');
            userProperties.append(value);
            userProperties.append('\n');
        }
        try {
            return new Configuration(ConfigurationSource.COMMAND_LINE_ARGS,
                                     "command-line",
                                     userProperties.toString());
        } catch (IOException e) {
            throw new AgentException("Failed to read property user configuration:%s",
                                     e.getMessage());
        }
    }

    /**
     * Read properties from java application arguments
     */
    public static Properties parseCommandLineArgs(String commandLineArgPrefix) {
        Properties args = new Properties();

        final String applicationArg = "-D" + commandLineArgPrefix;
        for (String arg : Helper.getCommandLineInputArgs()) {
            if (!arg.startsWith(applicationArg)) {
                continue;
            }

            String nameAndValue = arg.substring(applicationArg.length());
            if (StringUtils.isEmpty(nameAndValue)) {
                continue;
            }

            int assignmentIndex = nameAndValue.indexOf('=');
            if (assignmentIndex == -1) {
                continue;
            }
            args.put(nameAndValue.substring(0, assignmentIndex).trim(),
                     nameAndValue.substring(assignmentIndex + 1).trim());
        }

        return args;
    }
}
