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

package org.bithon.agent.plugin.spring.mvc;

import org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptor;
import org.bithon.agent.instrumentation.aop.interceptor.descriptor.MethodPointCutDescriptorBuilder;
import org.bithon.agent.instrumentation.aop.interceptor.matcher.Matchers;
import org.bithon.agent.instrumentation.aop.interceptor.plugin.IPlugin;

import java.util.Arrays;
import java.util.List;

import static org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptorBuilder.forClass;

/**
 * @author frankchen
 */
public class SpringMvcPlugin implements IPlugin {

    @Override
    public List<InterceptorDescriptor> getInterceptors() {

        return Arrays.asList(
            forClass("feign.SynchronousMethodHandler$Factory")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethod(Matchers.withName("create")
                                                                     .and(Matchers.takesArgument(1, "feign.MethodMetadata")))
                                                   .to("org.bithon.agent.plugin.spring.mvc.feign.SynchronousMethodHandlerFactory$Create")
                ),
            forClass("feign.SynchronousMethodHandler")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onAllMethods("invoke")
                                                   .to("org.bithon.agent.plugin.spring.mvc.feign.SynchronousMethodHandler$Invoke")
                ),

            forClass("org.springframework.web.client.RestTemplate")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onAllMethods("doExecute")
                                                   .to("org.bithon.agent.plugin.spring.mvc.RestTemplate$Execute"),

                    MethodPointCutDescriptorBuilder.build()
                                                   .onAllMethods("handleResponse")
                                                   .to("org.bithon.agent.plugin.spring.mvc.RestTemplate$HandleResponse")
                )
        );
    }
}
