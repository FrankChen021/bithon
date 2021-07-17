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

package com.sbss.bithon.agent.plugin.httpclient.apache;

import com.sbss.bithon.agent.core.aop.descriptor.InterceptorDescriptor;
import com.sbss.bithon.agent.core.aop.descriptor.MethodPointCutDescriptorBuilder;
import com.sbss.bithon.agent.core.plugin.AbstractPlugin;

import java.util.Arrays;
import java.util.List;

import static com.sbss.bithon.agent.core.aop.descriptor.InterceptorDescriptorBuilder.forClass;

/**
 * @author frankchen
 */
public class ApacheHttpClientPlugin extends AbstractPlugin {

    @Override
    public List<InterceptorDescriptor> getInterceptors() {
        return Arrays.asList(
            //
            // metrics
            //
            forClass("org.apache.http.impl.client.InternalHttpClient")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndArgs("doExecute",
                                                                    "org.apache.http.HttpHost",
                                                                    "org.apache.http.HttpRequest",
                                                                    "org.apache.http.protocol.HttpContext")
                                                   .to("com.sbss.bithon.agent.plugin.httpclient.apache.metrics.HttpClientExecuteInterceptor")
                ),

            forClass("org.apache.http.impl.execchain.RedirectExec")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndArgs("execute",
                                                                    "org.apache.http.conn.routing.HttpRoute",
                                                                    "org.apache.http.client.methods.HttpRequestWrapper",
                                                                    "org.apache.http.client.protocol.HttpClientContext",
                                                                    "org.apache.http.client.methods.HttpExecutionAware")
                                                   .to("com.sbss.bithon.agent.plugin.httpclient.apache.metrics.HttpClientExecuteInterceptor")
                ),

            forClass("org.apache.http.impl.execchain.MinimalClientExec")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndArgs("execute",
                                                                    "org.apache.http.conn.routing.HttpRoute",
                                                                    "org.apache.http.client.methods.HttpRequestWrapper",
                                                                    "org.apache.http.client.protocol.HttpClientContext",
                                                                    "org.apache.http.client.methods.HttpExecutionAware")
                                                   .to("com.sbss.bithon.agent.plugin.httpclient.apache.metrics.HttpClientExecuteInterceptor")
                ),

            forClass("org.apache.http.impl.client.DefaultRequestDirector")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndArgs("execute",
                                                                    "org.apache.http.HttpHost",
                                                                    "org.apache.http.HttpRequest",
                                                                    "org.apache.http.protocol.HttpContext")
                                                   .to("com.sbss.bithon.agent.plugin.httpclient.apache.metrics.DefaultRequestDirectorExecute"),

                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndNoArgs("releaseConnection")
                                                   .to("com.sbss.bithon.agent.plugin.httpclient.apache.metrics.DefaultRequestDirectorReleaseConnection")
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
                                                   .to("com.sbss.bithon.agent.plugin.httpclient.apache.trace.HttpRequestInterceptor")
                )
        );
    }
}
