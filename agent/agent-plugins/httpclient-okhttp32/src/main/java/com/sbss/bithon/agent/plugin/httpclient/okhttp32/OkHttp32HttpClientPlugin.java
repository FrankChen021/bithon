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

package com.sbss.bithon.agent.plugin.httpclient.okhttp32;

import com.sbss.bithon.agent.core.aop.descriptor.InterceptorDescriptor;
import com.sbss.bithon.agent.core.aop.descriptor.MethodPointCutDescriptorBuilder;
import com.sbss.bithon.agent.core.plugin.IPlugin;

import java.util.Collections;
import java.util.List;

import static com.sbss.bithon.agent.core.aop.descriptor.InterceptorDescriptorBuilder.forClass;

/**
 * OkHttp3, for version < 3.2
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
                                                   .to("com.sbss.bithon.agent.plugin.httpclient.okhttp32.OkHttp32Interceptor")
                )

            /*
            forClass("okhttp3.internal.http.BridgeInterceptor")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                        .onAllMethods("intercept")
                        .to("com.sbss.commons.agent.plugin.httpclient.okhttp32.OkHttp32TraceInterceptorHandler")
                ),

            forClass("okhttp3.Request$Builder")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                        .onAllMethods("build")
                        .to("com.sbss.commons.agent.plugin.httpclient.okhttp32.OkHttp32TraceRequestHandler")
                )
             */
        );
    }
}
