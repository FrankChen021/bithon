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

package org.bithon.agent.plugin.httpclient.okhttp32;

import org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptor;
import org.bithon.agent.instrumentation.aop.interceptor.matcher.Matchers;
import org.bithon.agent.instrumentation.aop.interceptor.plugin.IPlugin;
import org.bithon.shaded.net.bytebuddy.description.modifier.Visibility;

import java.util.Arrays;
import java.util.List;

import static org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptorBuilder.forClass;

/**
 * OkHttp3, for version &lt; 3.2
 *
 * @author frankchen
 */
public class OkHttp32HttpClientPlugin implements IPlugin {

    @Override
    public List<InterceptorDescriptor> getInterceptors() {

        return Arrays.asList(
            forClass("okhttp3.RealCall")
                .onMethodAndArgs("getResponseWithInterceptorChain",
                                 "boolean")
                .interceptedBy("org.bithon.agent.plugin.httpclient.okhttp32.RealCall$GetResponseWithInterceptorChain")
                .build(),

            forClass("okhttp3.internal.io.RealConnection")
                .onMethodAndArgs("connect")
                .interceptedBy("org.bithon.agent.plugin.httpclient.okhttp32.RealConnection$Connect")
                .build(),

            // 4.4+
            forClass("okhttp3.internal.connection.RealCall")
                // ProGuard has obfuscated OKHttp, '$okhttp' suffix is appended during compilation.
                // So we need to add this suffix to make sure it matches the method in the byte code
                .onMethodAndNoArgs("getResponseWithInterceptorChain$okhttp")
                .interceptedBy("org.bithon.agent.plugin.httpclient.okhttp32.RealCall$GetResponseWithInterceptorChain")
                .build(),

            forClass("okhttp3.internal.connection.RealConnection")
                // The connect method is not obfuscated
                .onMethod(Matchers.name("connect").and(Matchers.visibility(Visibility.PUBLIC)))
                .interceptedBy("org.bithon.agent.plugin.httpclient.okhttp32.RealConnection$Connect")
                .build()
        );
    }
}
