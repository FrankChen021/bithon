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

package com.sbss.bithon.agent.plugin.bithon.sdk;

import com.sbss.bithon.agent.core.aop.descriptor.InterceptorDescriptor;
import com.sbss.bithon.agent.core.aop.descriptor.MethodPointCutDescriptorBuilder;
import com.sbss.bithon.agent.core.plugin.IPlugin;

import java.util.Collections;
import java.util.List;

import static com.sbss.bithon.agent.core.aop.descriptor.InterceptorDescriptorBuilder.forClass;

/**
 * @author Frank Chen
 * @date 2021-10-01
 */
public class BithonSdkPlugin implements IPlugin {

    @Override
    public List<InterceptorDescriptor> getInterceptors() {
        return Collections.singletonList(
            //
            // metrics
            //
            forClass("com.sbss.bithon.agent.sdk.metric.MetricRegistryFactory")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onAllMethods("create")
                                                   .replaceBy("com.sbss.bithon.agent.plugin.bithon.sdk.interceptor.MetricRegistryFactory$Create")
                )
        );
    }
}
