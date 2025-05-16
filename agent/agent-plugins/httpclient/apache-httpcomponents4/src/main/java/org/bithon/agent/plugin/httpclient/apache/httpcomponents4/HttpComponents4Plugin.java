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

package org.bithon.agent.plugin.httpclient.apache.httpcomponents4;

import org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptor;
import org.bithon.agent.instrumentation.aop.interceptor.plugin.IPlugin;

import java.util.Arrays;
import java.util.List;

import static org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptorBuilder.forClass;

/**
 * @author frankchen
 */
public class HttpComponents4Plugin implements IPlugin {

    @Override
    public List<InterceptorDescriptor> getInterceptors() {
        return Arrays.asList(
            //
            // "http client 4.3.4~4.5.3: InternalHttpClient"
            //
            forClass("org.apache.http.impl.client.InternalHttpClient")
                .onMethod("doExecute")
                .andArgs("org.apache.http.HttpHost", "org.apache.http.HttpRequest", "org.apache.http.protocol.HttpContext")
                .interceptedBy("org.bithon.agent.plugin.httpclient.apache.httpcomponents4.interceptor.InternalHttpClient$DoExecute")
                .build(),

            //
            // http client 4.3.4~4.5.3: RedirectExec"
            //
            forClass("org.apache.http.impl.execchain.RedirectExec")
                .onMethod("execute")
                .andArgs("org.apache.http.conn.routing.HttpRoute",
                         "org.apache.http.client.methods.HttpRequestWrapper",
                         "org.apache.http.client.protocol.HttpClientContext",
                         "org.apache.http.client.methods.HttpExecutionAware")
                .interceptedBy("org.bithon.agent.plugin.httpclient.apache.httpcomponents4.interceptor.RedirectExec$Execute")
                .build(),

            //
            // "http client 4.3.4~4.5.3: MinimalClientExec"
            //
            forClass("org.apache.http.impl.execchain.MinimalClientExec")
                .onMethod("execute")
                .andArgs("org.apache.http.conn.routing.HttpRoute",
                         "org.apache.http.client.methods.HttpRequestWrapper",
                         "org.apache.http.client.protocol.HttpClientContext",
                         "org.apache.http.client.methods.HttpExecutionAware")
                .interceptedBy("org.bithon.agent.plugin.httpclient.apache.httpcomponents4.interceptor.MinimalClientExec$Execute")
                .build(),

            forClass("org.apache.http.impl.client.DefaultRequestDirector")
                .onMethod("execute")
                .andArgs("org.apache.http.HttpHost",
                         "org.apache.http.HttpRequest",
                         "org.apache.http.protocol.HttpContext")
                .interceptedBy("org.bithon.agent.plugin.httpclient.apache.httpcomponents4.interceptor.DefaultRequestDirector$Execute")

                .onMethod("releaseConnection")
                .andNoArgs()
                .interceptedBy("org.bithon.agent.plugin.httpclient.apache.httpcomponents4.interceptor.DefaultRequestDirector$ReleaseConnection")
                .build(),

            //
            // tracing
            //
            forClass("org.apache.http.protocol.HttpRequestExecutor")
                .onMethod("execute")
                .andArgs("org.apache.http.HttpRequest",
                         "org.apache.http.HttpClientConnection",
                         "org.apache.http.protocol.HttpContext")
                .interceptedBy("org.bithon.agent.plugin.httpclient.apache.httpcomponents4.interceptor.HttpRequestExecutor$Execute")
                .build(),

            // 4.3 and before
            forClass("org.apache.http.impl.conn.DefaultClientConnectionOperator")
                .onMethod("openConnection")
                .interceptedBy("org.bithon.agent.plugin.httpclient.apache.httpcomponents4.interceptor.DefaultClientConnectionOperator$OpenConnection")
                .build(),

            // Since 4.4
            forClass("org.apache.http.impl.conn.DefaultHttpClientConnectionOperator")
                .onMethod("connect")
                .interceptedBy("org.bithon.agent.plugin.httpclient.apache.httpcomponents4.interceptor.DefaultHttpClientConnectionOperator$Connect")
                .build()
        );
    }
}
