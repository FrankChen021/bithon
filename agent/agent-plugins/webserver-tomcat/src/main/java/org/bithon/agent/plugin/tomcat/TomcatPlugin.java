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

package org.bithon.agent.plugin.tomcat;

import org.bithon.agent.core.interceptor.plugin.IPlugin;
import org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptor;
import org.bithon.agent.instrumentation.aop.interceptor.descriptor.MethodPointCutDescriptorBuilder;

import java.util.Arrays;
import java.util.List;

import static org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptorBuilder.forClass;

/**
 * @author frankchen
 */
public class TomcatPlugin implements IPlugin {

    @Override
    public List<InterceptorDescriptor> getInterceptors() {

        return Arrays.asList(
            //web server
            forClass("org.apache.tomcat.util.net.AbstractEndpoint")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onAllMethods("start")
                                                   .to("org.bithon.agent.plugin.tomcat.interceptor.AbstractEndpoint$Start")
                ),

            // statistics
            // differ from Trace below since it depends on different response object
            forClass("org.apache.catalina.connector.CoyoteAdapter")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onAllMethods("service")
                                                   .onArgs("org.apache.coyote.Request", "org.apache.coyote.Response")
                                                   .to("org.bithon.agent.plugin.tomcat.interceptor.CoyoteAdapter$Service")
                ),

            //exception
            forClass("org.apache.catalina.core.StandardWrapperValve")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onAllMethods("exception")
                                                   .onArgs("org.apache.catalina.connector.Request",
                                                           "org.apache.catalina.connector.Response",
                                                           "java.lang.Throwable")
                                                   .to("org.bithon.agent.plugin.tomcat.interceptor.StandardWrapperValve$Exception")
                ),

            //trace
            forClass("org.apache.catalina.core.StandardHostValve")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onAllMethods("invoke")
                                                   .onArgs("org.apache.catalina.connector.Request",
                                                           "org.apache.catalina.connector.Response")
                                                   .to("org.bithon.agent.plugin.tomcat.interceptor.StandardHostValve$Invoke")
                ),

            forClass("org.apache.catalina.core.StandardContext")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onDefaultConstructor()
                                                   .to("org.bithon.agent.plugin.tomcat.interceptor.StandardContext$Ctor")
                )
        );
    }
}
