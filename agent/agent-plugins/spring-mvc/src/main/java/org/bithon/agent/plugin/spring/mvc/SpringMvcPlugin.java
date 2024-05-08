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
                .onMethod(Matchers.name("create")
                                  .and(Matchers.takesArgument(1, "feign.MethodMetadata")))
                .interceptedBy("org.bithon.agent.plugin.spring.mvc.feign.SynchronousMethodHandlerFactory$Create")
                .build(),

            forClass("feign.SynchronousMethodHandler")
                .onMethod("invoke")
                .interceptedBy("org.bithon.agent.plugin.spring.mvc.feign.SynchronousMethodHandler$Invoke")
                .build(),

            forClass("org.springframework.web.client.RestTemplate")
                .onMethod("doExecute")
                .interceptedBy("org.bithon.agent.plugin.spring.mvc.RestTemplate$Execute")
                .onMethod("handleResponse")
                .interceptedBy("org.bithon.agent.plugin.spring.mvc.RestTemplate$HandleResponse")
                .build(),

            forClass("org.springframework.web.method.support.InvocableHandlerMethod")
                .onMethod("doInvoke")
                .interceptedBy("org.bithon.agent.plugin.spring.mvc.InvocableHandlerMethod$DoInvoke")
                .build()
        );
    }
}
