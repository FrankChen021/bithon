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

package org.bithon.agent.plugin.httpserver.tomcat;

import org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptor;
import org.bithon.agent.instrumentation.aop.interceptor.plugin.IPlugin;

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
            // web server
            forClass("org.apache.tomcat.util.net.AbstractEndpoint")
                .onMethod("start")
                .interceptedBy("org.bithon.agent.plugin.httpserver.tomcat.interceptor.AbstractEndpoint$Start")
                .build(),

            // statistics
            // differ from the Trace below since it depends on different response objects
            forClass("org.apache.catalina.connector.CoyoteAdapter")
                .onMethod("service")
                .andArgs("org.apache.coyote.Request", "org.apache.coyote.Response")
                .interceptedBy("org.bithon.agent.plugin.httpserver.tomcat.interceptor.CoyoteAdapter$Service")
                .build(),

            // exception
            forClass("org.apache.catalina.core.StandardWrapperValve")
                .onMethod("exception")
                .andArgs("org.apache.catalina.connector.Request",
                         "org.apache.catalina.connector.Response",
                         "java.lang.Throwable")
                .interceptedBy("org.bithon.agent.plugin.httpserver.tomcat.interceptor.StandardWrapperValve$Exception")
                .build(),

            // trace
            forClass("org.apache.catalina.core.StandardHostValve")
                .onMethod("invoke")
                .andArgs("org.apache.catalina.connector.Request",
                         "org.apache.catalina.connector.Response")
                .interceptedBy("org.bithon.agent.plugin.httpserver.tomcat.interceptor.StandardHostValve$Invoke")
                .build(),

            forClass("org.apache.catalina.core.StandardContext")
                .onDefaultConstructor()
                .interceptedBy("org.bithon.agent.plugin.httpserver.tomcat.interceptor.StandardContext$Ctor")
                .build()
        );
    }
}
