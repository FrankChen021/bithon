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

package org.bithon.agent.plugin.jdbc.druid.config;

import org.bithon.agent.core.config.ConfigurationProperties;
import shaded.com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Frank Chen
 * @date 23/1/22 7:20 PM
 */
@ConfigurationProperties(prefix = "agent.plugin.jdbc.druid")
public class DruidPluginConfig {

    @JsonProperty
    private boolean isSQLMetricEnabled = false;

    public void setSQLMetricEnabled(boolean isSQLMetricEnabled) {
        this.isSQLMetricEnabled = isSQLMetricEnabled;
    }

    public boolean isSQLMetricEnabled() {
        return isSQLMetricEnabled;
    }
}
