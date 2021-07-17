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

package com.sbss.bithon.agent.plugin.tomcat;

import com.sbss.bithon.agent.core.aop.descriptor.InterceptorDescriptor;
import com.sbss.bithon.agent.core.aop.descriptor.MethodPointCutDescriptorBuilder;
import com.sbss.bithon.agent.core.plugin.IPlugin;

import java.util.Arrays;
import java.util.List;

import static com.sbss.bithon.agent.core.aop.descriptor.InterceptorDescriptorBuilder.forClass;

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
                                                   .to("com.sbss.bithon.agent.plugin.tomcat.interceptor.AbstractEndpointStart")
                ),

            // statistics
            // differ from Trace below since it depends on different response object
            forClass("org.apache.catalina.connector.CoyoteAdapter")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onAllMethods("service")
                                                   .onArgs("org.apache.coyote.Request", "org.apache.coyote.Response")
                                                   .to("com.sbss.bithon.agent.plugin.tomcat.interceptor.CoyoteAdapterService")
                ),

            //exception
            forClass("org.apache.catalina.core.StandardWrapperValve")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onAllMethods("exception")
                                                   .onArgs("org.apache.catalina.connector.Request",
                                                           "org.apache.catalina.connector.Response",
                                                           "java.lang.Throwable")
                                                   .to("com.sbss.bithon.agent.plugin.tomcat.interceptor.StandardWrapperValveException")
                ),

            //trace
            forClass("org.apache.catalina.core.StandardHostValve")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onAllMethods("invoke")
                                                   .onArgs("org.apache.catalina.connector.Request",
                                                           "org.apache.catalina.connector.Response")
                                                   .to("com.sbss.bithon.agent.plugin.tomcat.interceptor.StandardHostValveInvoke")
                )
        );
    }
}
