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

package org.bithon.agent.plugin.jetty;

import org.bithon.agent.core.aop.descriptor.BithonClassDescriptor;
import org.bithon.agent.core.aop.descriptor.InterceptorDescriptor;
import org.bithon.agent.core.aop.descriptor.MethodPointCutDescriptorBuilder;
import org.bithon.agent.core.plugin.IPlugin;

import java.util.Arrays;
import java.util.List;

import static org.bithon.agent.core.aop.descriptor.InterceptorDescriptorBuilder.forClass;

/**
 * @author frankchen
 */
public class JettyPlugin implements IPlugin {

    @Override
    public BithonClassDescriptor getBithonClassDescriptor() {
        return BithonClassDescriptor.of("org.eclipse.jetty.server.Request", true);
    }

    @Override
    public List<InterceptorDescriptor> getInterceptors() {
        return Arrays.asList(
            forClass("org.eclipse.jetty.server.AbstractConnector")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndNoArgs("doStart")
                                                   .to("org.bithon.agent.plugin.jetty.interceptor.AbstractConnectorDoStart")
                ),

            forClass("org.eclipse.jetty.util.thread.QueuedThreadPool")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndNoArgs("doStart")
                                                   .to("org.bithon.agent.plugin.jetty.interceptor.QueuedThreadPoolDoStart")
                ),

            forClass("org.eclipse.jetty.server.HttpChannel")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndNoArgs("handle")
                                                   .to("org.bithon.agent.plugin.jetty.interceptor.HttpChannel$Handle"),

                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndNoArgs("onCompleted")
                                                   .to("org.bithon.agent.plugin.jetty.interceptor.HttpChannel$OnCompleted")
                )
        );
    }
}
