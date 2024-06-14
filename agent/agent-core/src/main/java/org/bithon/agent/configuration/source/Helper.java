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

import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.Map;

/**
 * Wrap of some JDK methods to support easier mock in test cases
 *
 * @author Frank Chen
 * @date 20/2/24 4:39 pm
 */
public class Helper {
    public static List<String> getCommandLineInputArgs() {
        return ManagementFactory.getRuntimeMXBean().getInputArguments();
    }

    public static Map<String, String> getEnvironmentVariables() {
        return System.getenv();
    }
}
