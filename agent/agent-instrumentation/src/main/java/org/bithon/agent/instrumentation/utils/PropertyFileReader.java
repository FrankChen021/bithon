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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Read a property file packed in a jar or under the classpath.
 *
 * @author frank.chen021@outlook.com
 * @date 2025/8/10 21:53
 */
public class PropertyFileReader {
    @SuppressFBWarnings(value = "RCN_REDUNDANT_NULLCHECK_OF_NULL_VALUE")
    public static Properties read(ClassLoader classLoader, String propertyFile) throws IOException {
        try (InputStream inputStream = classLoader.getResourceAsStream(propertyFile)) {
            if (inputStream == null) {
                return new Properties();
            }

            Properties properties = new Properties();
            properties.load(inputStream);
            return properties;
        }
    }
}
