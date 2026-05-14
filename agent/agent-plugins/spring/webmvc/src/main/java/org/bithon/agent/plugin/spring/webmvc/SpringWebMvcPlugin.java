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

package org.bithon.agent.plugin.spring.webmvc;

import org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptor;
import org.bithon.agent.instrumentation.aop.interceptor.plugin.IPlugin;
import org.bithon.agent.instrumentation.aop.interceptor.precondition.ClassPackageVersionPrecondition;
import org.bithon.agent.instrumentation.aop.interceptor.precondition.IInterceptorPrecondition;
import org.bithon.agent.instrumentation.aop.interceptor.precondition.PropertyFileValuePrecondition;

import java.util.Arrays;
import java.util.List;

import static org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptorBuilder.forClass;

/**
 * @author frankchen
 */
public class SpringWebMvcPlugin implements IPlugin {

    @Override
    public List<InterceptorDescriptor> getInterceptors() {

        return Arrays.asList(

            forClass("org.springframework.web.client.RestTemplate")
                .when(springVersionLessThan7())
                .onMethod("doExecute")
                .interceptedBy("org.bithon.agent.plugin.spring.webmvc.rs.RestTemplate$Execute")
                .onMethod("handleResponse")
                .interceptedBy("org.bithon.agent.plugin.spring.webmvc.rs.RestTemplate$HandleResponse")
                .build(),

            forClass("org.springframework.web.method.support.InvocableHandlerMethod")
                .when(springVersionLessThan7())
                .onMethod("doInvoke")
                .interceptedBy("org.bithon.agent.plugin.spring.webmvc.controller.InvocableHandlerMethod$DoInvoke")
                .build()
        );
    }

    private static IInterceptorPrecondition springVersionLessThan7() {
        return new ClassPackageVersionPrecondition("org.springframework.http.ResponseEntity",
                                                   PropertyFileValuePrecondition.VersionLT.of("7.0.0"));
    }
}
