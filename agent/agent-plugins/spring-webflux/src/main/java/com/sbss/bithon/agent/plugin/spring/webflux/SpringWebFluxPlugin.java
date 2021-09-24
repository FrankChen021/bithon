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

package com.sbss.bithon.agent.plugin.spring.webflux;

import com.sbss.bithon.agent.core.aop.descriptor.BithonClassDescriptor;
import com.sbss.bithon.agent.core.aop.descriptor.InterceptorDescriptor;
import com.sbss.bithon.agent.core.aop.descriptor.MethodPointCutDescriptorBuilder;
import com.sbss.bithon.agent.core.plugin.IPlugin;

import java.util.Collections;
import java.util.List;

import static com.sbss.bithon.agent.core.aop.descriptor.InterceptorDescriptorBuilder.forClass;

/**
 * @author frankchen
 * @date 2021-09-22 23:36
 */
public class SpringWebFluxPlugin implements IPlugin {

    @Override
    public BithonClassDescriptor getBithonClassDescriptor() {
        return BithonClassDescriptor.of("org.springframework.web.server.adapter.DefaultServerWebExchange");
    }

    @Override
    public List<InterceptorDescriptor> getInterceptors() {
        return Collections.singletonList(
            forClass("org.springframework.web.reactive.DispatcherHandler")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onAllMethods("handle")
                                                   .to("com.sbss.bithon.agent.plugin.spring.webflux.interceptor.Handle")

                    /*
                    MethodPointCutDescriptorBuilder.build()
                                                   .onAllMethods("handleResult")
                                                   .to("com.sbss.bithon.agent.plugin.spring.webflux.interceptor.HandleResult")
                     */
                )
        );
    }
}
