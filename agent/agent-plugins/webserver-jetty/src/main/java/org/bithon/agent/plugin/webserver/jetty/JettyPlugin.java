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

package org.bithon.agent.plugin.webserver.jetty;

import org.bithon.agent.instrumentation.aop.interceptor.descriptor.BithonClassDescriptor;
import org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptor;
import org.bithon.agent.instrumentation.aop.interceptor.plugin.IPlugin;

import java.util.Arrays;
import java.util.List;

import static org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptorBuilder.forClass;

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
                .onMethod("doStart")
                .andNoArgs()
                .interceptedBy("org.bithon.agent.plugin.webserver.jetty.interceptor.AbstractConnector$DoStart")
                .build(),

            forClass("org.eclipse.jetty.util.thread.QueuedThreadPool")
                .onMethod("doStart")
                .andNoArgs()
                .interceptedBy("org.bithon.agent.plugin.webserver.jetty.interceptor.QueuedThreadPool$DoStart")
                .build(),

            forClass("org.eclipse.jetty.server.HttpChannel")
                .onMethod("handle")
                .andNoArgs()
                .interceptedBy("org.bithon.agent.plugin.webserver.jetty.interceptor.HttpChannel$Handle")

                .onMethod("onCompleted")
                .andNoArgs()
                .interceptedBy("org.bithon.agent.plugin.webserver.jetty.interceptor.HttpChannel$OnCompleted")

                .onMethod("handleException")
                .andArgs("java.lang.Throwable")
                .interceptedBy("org.bithon.agent.plugin.webserver.jetty.interceptor.HttpChannel$HandleException")
                .build()
        );
    }
}
