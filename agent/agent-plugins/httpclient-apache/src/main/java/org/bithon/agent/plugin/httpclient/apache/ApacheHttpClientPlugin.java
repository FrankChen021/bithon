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

package org.bithon.agent.plugin.httpclient.apache;

import org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptor;
import org.bithon.agent.instrumentation.aop.interceptor.descriptor.MethodPointCutDescriptorBuilder;
import org.bithon.agent.instrumentation.aop.interceptor.plugin.IPlugin;

import java.util.Arrays;
import java.util.List;

import static org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptorBuilder.forClass;

/**
 * @author frankchen
 */
public class ApacheHttpClientPlugin implements IPlugin {

    @Override
    public List<InterceptorDescriptor> getInterceptors() {
        return Arrays.asList(
            //
            // "http client 4.3.4~4.5.3: InternalHttpClient"
            //
            forClass("org.apache.http.impl.client.InternalHttpClient")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndArgs("doExecute",
                                                                    "org.apache.http.HttpHost",
                                                                    "org.apache.http.HttpRequest",
                                                                    "org.apache.http.protocol.HttpContext")
                                                   .to("org.bithon.agent.plugin.httpclient.apache.interceptor.InternalHttpClient$DoExecute")
                        ),

            //
            // http client 4.3.4~4.5.3: RedirectExec"
            //
            forClass("org.apache.http.impl.execchain.RedirectExec")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndArgs("execute",
                                                                    "org.apache.http.conn.routing.HttpRoute",
                                                                    "org.apache.http.client.methods.HttpRequestWrapper",
                                                                    "org.apache.http.client.protocol.HttpClientContext",
                                                                    "org.apache.http.client.methods.HttpExecutionAware")
                                                   .to("org.bithon.agent.plugin.httpclient.apache.interceptor.RedirectExec$Execute")
                        ),

            //
            // "http client 4.3.4~4.5.3: MinimalClientExec"
            //
            forClass("org.apache.http.impl.execchain.MinimalClientExec")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndArgs("execute",
                                                                    "org.apache.http.conn.routing.HttpRoute",
                                                                    "org.apache.http.client.methods.HttpRequestWrapper",
                                                                    "org.apache.http.client.protocol.HttpClientContext",
                                                                    "org.apache.http.client.methods.HttpExecutionAware")
                                                   .to("org.bithon.agent.plugin.httpclient.apache.interceptor.MinimalClientExec$Execute")
                        ),

            forClass("org.apache.http.impl.client.DefaultRequestDirector")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndArgs("execute",
                                                                    "org.apache.http.HttpHost",
                                                                    "org.apache.http.HttpRequest",
                                                                    "org.apache.http.protocol.HttpContext")
                                                   .to("org.bithon.agent.plugin.httpclient.apache.interceptor.DefaultRequestDirector$Execute"),

                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndNoArgs("releaseConnection")
                                                   .to("org.bithon.agent.plugin.httpclient.apache.interceptor.DefaultRequestDirector$ReleaseConnection")
                        ),

            //
            // tracing
            //
            forClass("org.apache.http.protocol.HttpRequestExecutor")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndArgs("execute",
                                                                    "org.apache.http.HttpRequest",
                                                                    "org.apache.http.HttpClientConnection",
                                                                    "org.apache.http.protocol.HttpContext")
                                                   .to("org.bithon.agent.plugin.httpclient.apache.interceptor.HttpRequestExecutor$Execute")
                        ),

            // 4.3 and before
            forClass("org.apache.http.impl.conn.DefaultClientConnectionOperator")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onAllMethods("openConnection")
                                                   .to("org.bithon.agent.plugin.httpclient.apache.interceptor.DefaultClientConnectionOperator$OpenConnection")
                        ),

            // Since 4.4
            forClass("org.apache.http.impl.conn.DefaultHttpClientConnectionOperator")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onAllMethods("connect")
                                                   .to("org.bithon.agent.plugin.httpclient.apache.interceptor.DefaultHttpClientConnectionOperator$Connect")
                        )
                            );
    }
}
