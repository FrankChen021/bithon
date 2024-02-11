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

package org.bithon.server.alerting.manager;

import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.alerting.manager.security.DefaultUserProvider;
import org.bithon.server.alerting.manager.security.IUserProvider;
import org.bithon.server.storage.alerting.AlertingStorageConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.env.Environment;

/**
 * @author Frank Chen
 * @date 12/11/21 6:18 pm
 */
@Configuration
@Conditional(ManagerModuleEnabler.class)
@ImportAutoConfiguration(value = {AlertingStorageConfiguration.class})
public class ManagerModuleAutoConfiguration {
    @Bean
    IUserProvider alertingModuleUserProvider(Environment environment) {
        boolean allowAnonymous;
        try {
            allowAnonymous = environment.getProperty("bithon.alerting.module.manager.allow-anonymous", Boolean.class, false);
        } catch (ConversionFailedException e) {
            throw new RuntimeException(StringUtils.format("Invalid value [%s] for 'bithon.alerting.module.manager.allow-anonymous' property. It must be a Boolean.",
                                                          environment.getProperty("bithon.alerting.module.manager.allow-anonymous")));
        }
        return new DefaultUserProvider(allowAnonymous);
    }
}
