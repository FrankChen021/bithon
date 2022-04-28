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

package org.bithon.server.alerting.admin;

import org.bithon.server.alerting.common.AlertingModule;
import org.bithon.server.storage.alerting.AlertingStorageConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Frank Chen
 * @date 12/11/21 6:18 pm
 */
@Configuration
@ConditionalOnProperty(prefix = "bithon.alerting.admin", name = "enabled", havingValue = "true")
@ImportAutoConfiguration(value = {AlertingStorageConfiguration.class})
public class AdminModuleAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    AlertingModule alertingModule() {
        return new AlertingModule();
    }
}
