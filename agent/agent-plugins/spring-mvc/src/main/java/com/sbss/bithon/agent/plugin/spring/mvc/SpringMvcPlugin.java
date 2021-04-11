/*
 *    Copyright 2020 bithon.cn
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

package com.sbss.bithon.agent.plugin.spring.mvc;

import com.sbss.bithon.agent.core.plugin.AbstractPlugin;
import com.sbss.bithon.agent.core.plugin.descriptor.InterceptorDescriptor;
import com.sbss.bithon.agent.core.plugin.descriptor.MethodPointCutDescriptorBuilder;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

import static com.sbss.bithon.agent.core.plugin.descriptor.InterceptorDescriptorBuilder.forClass;
import static shaded.net.bytebuddy.matcher.ElementMatchers.isAnnotatedWith;
import static shaded.net.bytebuddy.matcher.ElementMatchers.isPublic;
import static shaded.net.bytebuddy.matcher.ElementMatchers.named;
import static shaded.net.bytebuddy.matcher.ElementMatchers.not;

/**
 * @author frankchen
 */
public class SpringMvcPlugin extends AbstractPlugin {

    /**
     * {@link org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#createBeanInstance(String, RootBeanDefinition, Object[])}
     * {@link org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#applyBeanPostProcessorsBeforeInstantiation}
     * @return
     */
    @Override
    public List<InterceptorDescriptor> getInterceptors() {

        return Arrays.asList(

//            forClass(isAnnotatedWith(Component.class).or(isAnnotatedWith(Service.class)).or(isAnnotatedWith(Repository.class)))
//                .debug()
//                .methods(
//                    MethodPointCutDescriptorBuilder.build()
//                                                   .onMethod(isPublic().and(not(named("java.lang.Object.toString").or(named("java.lang.Object.hashCode")))))
//                                                   .to("com.sbss.bithon.agent.plugin.spring.mvc.SpringBeanInterceptor")
//                ),

            forClass(
                "org.springframework.web.servlet.handler.AbstractHandlerMethodMapping")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndArgs(
                                                       "registerHandlerMethod",
                                                       "java.lang.Object",
                                                       "java.lang.reflect.Method",
                                                       "T")
                                                   .to("com.sbss.bithon.agent.plugin.spring.mvc.MethodMatchingInterceptor")
                ),

            forClass(
                "org.springframework.web.servlet.handler.AbstractHandlerMethodMapping")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndArgs(
                                                       "registerHandlerMethod",
                                                       "java.lang.Object",
                                                       "java.lang.reflect.Method",
                                                       "T")
                                                   .to("com.sbss.bithon.agent.plugin.spring.mvc.MethodMatchingInterceptor")
                ),

            forClass("org.springframework.web.client.RestTemplate")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onAllMethods(
                                                       "execute")
                                                   .to("com.sbss.bithon.agent.plugin.spring.mvc.RestTemplateExecuteInterceptor")
                )
        );
    }
}
