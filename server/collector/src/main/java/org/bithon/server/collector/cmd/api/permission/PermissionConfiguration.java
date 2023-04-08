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

package org.bithon.server.collector.cmd.api.permission;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.List;

/**
 * A simple permission control on SET/WRITE commands to agent to ensure safety.
 *
 * @author frank.chen021@outlook.com
 * @date 2023/4/8 15:08
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "collector-controller.permission")
public class PermissionConfiguration {

    private List<PermissionRule> rules = Collections.emptyList();
}
