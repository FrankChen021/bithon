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
import org.bithon.agent.instrumentation.aop.interceptor.descriptor.MethodPointCutDescriptorBuilder;
import org.bithon.agent.instrumentation.aop.interceptor.plugin.IPlugin;

import java.util.Collections;
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

        return Collections.singletonList(
            forClass("okhttp3.RealCall")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndArgs("getResponseWithInterceptorChain",
                                                                    "boolean")
                                                   .to("org.bithon.agent.plugin.httpclient.okhttp32.RealCall$GetResponseWithInterceptorChain")
                )

            /*
            forClass("okhttp3.internal.http.BridgeInterceptor")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                        .onAllMethods("intercept")
                        .to("org.commons.agent.plugin.httpclient.okhttp32.OkHttp32TraceInterceptorHandler")
                ),

            forClass("okhttp3.Request$Builder")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                        .onAllMethods("build")
                        .to("org.commons.agent.plugin.httpclient.okhttp32.OkHttp32TraceRequestHandler")
                )
             */
        );
    }
}
