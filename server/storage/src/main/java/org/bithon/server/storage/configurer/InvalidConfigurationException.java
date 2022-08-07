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

package org.bithon.server.storage.configurer;

import org.bithon.component.commons.utils.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author frank.chen021@outlook.com
 * @date 17/12/21 10:17 AM
 */
public class InvalidConfigurationException extends RuntimeException {
    private InvalidConfigurationException(String s) {
        super(s);
    }

    /**
     * @param messagePattern the pattern must contain only one %s pattern which indicates the configuration item name
     */
    public static void throwIf(boolean isTrue,
                               String messagePattern,
                               Class<?> configurationClass,
                               String propertyName) {
        if (isTrue) {
            ConfigurationProperties prop = configurationClass.getAnnotation(ConfigurationProperties.class);

            // Spring may enhance the configuration class,
            // so we need to search its parent class to see if there's annotation class defined
            while (prop == null && configurationClass.getSuperclass() != null) {
                configurationClass = configurationClass.getSuperclass();
                prop = configurationClass.getAnnotation(ConfigurationProperties.class);
            }

            throw new InvalidConfigurationException(StringUtils.format(messagePattern,
                                                                       prop == null
                                                                       ? propertyName
                                                                       : prop.prefix() + "." + propertyName));
        }
    }
}
