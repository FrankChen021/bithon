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

package org.bithon.agent.plugin.spring.bean.installer;

import org.bithon.agent.configuration.annotation.ConfigurationProperties;
import org.bithon.agent.configuration.annotation.PropertyDescriptor;
import org.bithon.agent.observability.aop.BeanMethodAopInstaller;

/**
 * @author frank.chen021@outlook.com
 * @date 2024/11/3 11:33
 */
@ConfigurationProperties(path = "agent.plugin.spring.bean")
public class SpringBeanPluginConfig extends BeanMethodAopInstaller.BeanTransformationConfig {
    @PropertyDescriptor(
        value = "Whether to enable Spring Service/Repository/Component only. If true, only these components will be instrumented.",
        required = false
    )
    private boolean enableServiceComponentOnly = false;

    public boolean isEnableServiceComponentOnly() {
        return enableServiceComponentOnly;
    }

    public void setEnableServiceComponentOnly(boolean enableServiceComponentOnly) {
        this.enableServiceComponentOnly = enableServiceComponentOnly;
    }
}
