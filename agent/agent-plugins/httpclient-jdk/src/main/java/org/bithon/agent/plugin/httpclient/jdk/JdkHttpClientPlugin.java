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

package org.bithon.agent.plugin.httpclient.jdk;

import org.bithon.agent.core.aop.descriptor.InterceptorDescriptor;
import org.bithon.agent.core.aop.descriptor.MethodPointCutDescriptorBuilder;
import org.bithon.agent.core.aop.matcher.Matchers;
import org.bithon.agent.core.plugin.IPlugin;

import java.util.Arrays;
import java.util.List;

import static org.bithon.agent.core.aop.descriptor.InterceptorDescriptorBuilder.forBootstrapClass;
import static shaded.net.bytebuddy.matcher.ElementMatchers.isStatic;
import static shaded.net.bytebuddy.matcher.ElementMatchers.named;
import static shaded.net.bytebuddy.matcher.ElementMatchers.takesArguments;

/**
 * jdk http-connection plugin
 *
 * @author frankchen
 */
public class JdkHttpClientPlugin implements IPlugin {

    @Override
    public List<InterceptorDescriptor> getInterceptors() {
        return Arrays.asList(
            forBootstrapClass("sun.net.www.http.HttpClient")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethod(named("New")
                                                                 .and(isStatic())
                                                                 .and(takesArguments(5))
                                                                 .and(Matchers.takesArgument(4, "sun.net.www.protocol.http.HttpURLConnection")))
                                                   .to("org.bithon.agent.plugin.httpclient.jdk.interceptor.HttpClient$New"),

                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndArgs("writeRequests", "sun.net.www.MessageHeader")
                                                   .to("org.bithon.agent.plugin.httpclient.jdk.interceptor.HttpClient$WriteRequest"),

                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndArgs("writeRequests",
                                                                    "sun.net.www.MessageHeader",
                                                                    "sun.net.www.http.PosterOutputStream")
                                                   .to("org.bithon.agent.plugin.httpclient.jdk.interceptor.HttpClient$WriteRequest"),

                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethod(named("parseHTTP").and(Matchers.takesArgument(0, "sun.net.www.MessageHeader")))
                                                   .to("org.bithon.agent.plugin.httpclient.jdk.interceptor.HttpClient$ParseHttp")),

            // HttpsClient inherits from HttpClient
            forBootstrapClass("sun.net.www.protocol.https.HttpsClient")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethod(named("New")
                                                                 .and(isStatic())
                                                                 .and(takesArguments(7))
                                                                 // there're two overridden versions of 'New' both of which have 7 parameters
                                                                 // and they don't share the 3rd parameter
                                                                 .and(Matchers.takesArgument(3, "java.net.Proxy"))
                                                                 .and(Matchers.takesArgument(6,
                                                                                             "sun.net.www.protocol.http.HttpURLConnection")))
                                                   .to("org.bithon.agent.plugin.httpclient.jdk.interceptor.HttpsClient$New"))

        );
    }
}
