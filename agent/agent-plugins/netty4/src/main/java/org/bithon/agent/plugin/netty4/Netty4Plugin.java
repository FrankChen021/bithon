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

package org.bithon.agent.plugin.netty4;


import org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptor;
import org.bithon.agent.instrumentation.aop.interceptor.plugin.IPlugin;

import java.util.Arrays;
import java.util.List;

import static org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptorBuilder.forClass;


/**
 * @author frankchen
 */
public class Netty4Plugin implements IPlugin {

    @Override
    public List<InterceptorDescriptor> getInterceptors() {
        InterceptorDescriptor descriptor =
            forClass("io.netty.util.ResourceLeakDetector")
                .onMethod("reportUntracedLeak")
                .interceptedBy("org.bithon.agent.plugin.netty4.ResourceLeakDetector$ReportUntracedLeak")

                .onMethod("reportTracedLeak")
                .interceptedBy("org.bithon.agent.plugin.netty4.ResourceLeakDetector$ReportTracedLeak")

                .onMethod("needReport")
                .replacedBy("org.bithon.agent.plugin.netty4.ResourceLeakDetector$NeedReport")
                .build();

        return Arrays.asList(descriptor.withTargetClazz("org.apache.ratis.thirdparty.io.netty.util.ResourceLeakDetector"),
                             descriptor.withTargetClazz("org.bithon.shaded.io.netty.util.ResourceLeakDetector"),
                             descriptor);
    }
}
