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

package org.bithon.agent.plugin.apache.httpcomponents5;

import org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptor;
import org.bithon.agent.instrumentation.aop.interceptor.matcher.Matchers;
import org.bithon.agent.instrumentation.aop.interceptor.plugin.IPlugin;

import java.util.Arrays;
import java.util.List;

import static org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptorBuilder.forClass;

/**
 * @author frankchen
 */
public class HttpComponents5Plugin implements IPlugin {

    @Override
    public List<InterceptorDescriptor> getInterceptors() {
        return Arrays.asList(
            forClass("org.apache.hc.core5.http.impl.BasicHttpTransportMetrics")
                .hook()
                .onMethodAndNoArgs("getBytesTransferred")
                .to("org.bithon.agent.plugin.apache.httpcomponents5.interceptor.BasicHttpTransportMetrics$GetBytesTransferred")
                .build(),

            // Tracing http request
            forClass("org.apache.hc.core5.http.impl.io.HttpRequestExecutor")
                .hook()
                .onMethod(Matchers.name("execute").and(Matchers.takesArguments(4)))
                .to("org.bithon.agent.plugin.apache.httpcomponents5.interceptor.HttpRequestExecutor$Execute")
                .build(),

            // Tracing http connection connect
            forClass("org.apache.hc.client5.http.impl.io.DefaultHttpClientConnectionOperator")
                .hook()
                .onMethod(Matchers.name("connect").and(Matchers.takesArguments(7)))
                .to("org.bithon.agent.plugin.apache.httpcomponents5.interceptor.DefaultHttpClientConnectionOperator$Connect")
                .build()
        );
    }
}
