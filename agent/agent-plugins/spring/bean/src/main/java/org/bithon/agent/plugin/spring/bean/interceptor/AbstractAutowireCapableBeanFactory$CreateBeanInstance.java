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

package org.bithon.agent.plugin.spring.bean.interceptor;

import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AfterInterceptor;
import org.bithon.agent.plugin.spring.bean.installer.SpringBeanMethodAopInstaller;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.factory.support.RootBeanDefinition;

/**
 * {@link org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#createBeanInstance(String, RootBeanDefinition, Object[])}
 *
 * @author frank.chen021@outlook.com
 * @date 2021/4/11 20:48
 */
public class AbstractAutowireCapableBeanFactory$CreateBeanInstance extends AfterInterceptor {

    /**
     * Re-transform the class of the bean
     */
    @Override
    public void after(AopContext aopContext) {
        if (aopContext.getReturning() == null || aopContext.hasException()) {
            return;
        }

        String beanName = aopContext.getArgAs(0);
        if (beanName == null) {
            return;
        }

        BeanWrapper result = aopContext.getReturningAs();
        Object beanInstance = result.getWrappedInstance();
        SpringBeanMethodAopInstaller.installFor(beanInstance.getClass());
    }
}
