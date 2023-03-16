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

import org.bithon.agent.bootstrap.aop.advice.IAdviceAopTemplate;
import org.bithon.agent.core.aop.AopClassHelper;
import org.bithon.agent.core.aop.installer.BeanMethodAopInstaller;
import org.bithon.agent.core.config.ConfigurationManager;
import org.bithon.agent.plugin.spring.bean.interceptor.BeanMethod$Invoke;
import org.bithon.shaded.net.bytebuddy.asm.Advice;
import org.bithon.shaded.net.bytebuddy.dynamic.ClassFileLocator;
import org.bithon.shaded.net.bytebuddy.dynamic.DynamicType;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/7/10 13:05
 */
public class BeanMethodAopInstallerHelper {

    static BeanMethodAopInstaller.BeanTransformationConfig transformationConfig;

    private static DynamicType.Unloaded<?> targetAopClass;

    /**
     * for the interceptors of spring-beans, they perform same function,
     * So we use one interceptor for all spring-beans, and generates an Aop for that interceptor
     */
    public static void initialize() {
        transformationConfig = ConfigurationManager.getInstance()
                                                   .getConfig("agent.plugin.spring.bean", BeanMethodAopInstaller.BeanTransformationConfig.class);

        String targetAopClassName = BeanMethodAopInstallerHelper.class.getPackage().getName() + ".SpringBeanMethodAop";

        targetAopClass = AopClassHelper.generateAopClass(IAdviceAopTemplate.class,
                                                         targetAopClassName,
                                                         BeanMethod$Invoke.class.getName(),
                                                         true);
        AopClassHelper.inject(targetAopClass);
    }

    public static void install(Class<?> targetClass) {
        if (BeanMethod$Invoke.AnnotationHelper.getOrCreateComponentName(targetClass) != null) {
            BeanMethodAopInstaller.install(targetClass,
                                           Advice.to(targetAopClass.getTypeDescription(), ClassFileLocator.Simple.of(targetAopClass.getAllTypes())),
                                           transformationConfig);
        }
    }
}
