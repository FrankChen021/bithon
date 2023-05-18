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

import org.bithon.agent.configuration.ConfigurationManager;
import org.bithon.agent.observability.aop.BeanMethodAopInstaller;
import org.bithon.agent.plugin.spring.bean.interceptor.BeanMethod$Invoke;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/7/10 13:05
 */
public class BeanMethodAopInstallerHelper {

    public static void install(Class<?> targetClass) {

        BeanMethodAopInstaller.BeanTransformationConfig transformationConfig = ConfigurationManager.getInstance()
                                                                                                   .getConfig("agent.plugin.spring.bean",
                                                                                                              BeanMethodAopInstaller.BeanTransformationConfig.class,
                                                                                                              true);

        BeanMethodAopInstaller.install(targetClass,
                                       BeanMethod$Invoke.class.getName(),
                                       transformationConfig);
    }
}
