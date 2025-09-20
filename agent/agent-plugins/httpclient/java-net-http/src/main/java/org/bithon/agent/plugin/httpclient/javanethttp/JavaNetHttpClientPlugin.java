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

package org.bithon.agent.plugin.httpclient.javanethttp;

import org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptor;
import org.bithon.agent.instrumentation.aop.interceptor.plugin.IPlugin;
import org.bithon.agent.instrumentation.aop.interceptor.precondition.IInterceptorPrecondition;
import org.bithon.agent.instrumentation.aop.interceptor.precondition.JdkVersionPrecondition;
import org.bithon.shaded.net.bytebuddy.description.modifier.Visibility;

import java.util.Arrays;
import java.util.List;

import static org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptorBuilder.forClass;

/**
 * Plugin for instrumenting {@link java.net.http.HttpClient} (introduced in Java 11)
 * <p>
 * This plugin intercepts the following key methods:
 * - HttpClient.send() and sendAsync() - to start tracing spans and collect request metrics
 * - HttpResponse handling - to collect response metrics and complete tracing spans
 *
 * @author frank.chen021@outlook.com
 * @date 2024/12/19
 */
public class JavaNetHttpClientPlugin implements IPlugin {

    @Override
    public IInterceptorPrecondition getPreconditions() {
        return JdkVersionPrecondition.gte(11);
    }

    @Override
    public List<InterceptorDescriptor> getInterceptors() {
        return Arrays.asList(

            // Intercept synchronous HTTP requests
            forClass("jdk.internal.net.http.HttpClientImpl")
                .onMethod("send")
                .andArgs("java.net.http.HttpRequest", "java.net.http.HttpResponse$BodyHandler")
                .interceptedBy("org.bithon.agent.plugin.httpclient.javanethttp.interceptor.HttpClient$Send")
                .build(),

            // Intercept asynchronous HTTP requests
            forClass("jdk.internal.net.http.HttpClientImpl")
                .onMethod("sendAsync")
                .andArgsSize(4)
                .andVisibility(Visibility.PRIVATE)
                .interceptedBy("org.bithon.agent.plugin.httpclient.javanethttp.interceptor.HttpClient$SendAsync")
                .build()
        );
    }
}
