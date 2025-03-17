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

package org.bithon.agent.plugin.httpserver.jetty12;

import org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptor;
import org.bithon.agent.instrumentation.aop.interceptor.matcher.Matchers;
import org.bithon.agent.instrumentation.aop.interceptor.plugin.IPlugin;
import org.bithon.agent.instrumentation.aop.interceptor.precondition.IInterceptorPrecondition;
import org.bithon.agent.instrumentation.aop.interceptor.precondition.PropertyFileValuePrecondition;

import java.util.Arrays;
import java.util.List;

import static org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptorBuilder.forClass;

/**
 * @author frankchen
 */
public class Jetty12Plugin implements IPlugin {

    @Override
    public IInterceptorPrecondition getPreconditions() {
        return new PropertyFileValuePrecondition("META-INF/maven/org.eclipse.jetty/jetty-server/pom.properties",
                                                 "version",
                                                 PropertyFileValuePrecondition.VersionGTE.of("12.0.0"));
    }

    @Override
    public List<InterceptorDescriptor> getInterceptors() {
        return Arrays.asList(
            forClass("org.eclipse.jetty.server.AbstractConnector")
                .onMethod("doStart")
                .andNoArgs()
                .interceptedBy("org.bithon.agent.plugin.httpserver.jetty12.interceptor.AbstractConnector$DoStart")
                .build(),

            forClass("org.eclipse.jetty.util.thread.QueuedThreadPool")
                .onMethod("doStart")
                .andNoArgs()
                .interceptedBy("org.bithon.agent.plugin.httpserver.jetty12.interceptor.QueuedThreadPool$DoStart")
                .build(),

            forClass("org.eclipse.jetty.server.internal.HttpChannelState")
                .onMethod("onRequest")
                .andArgs("org.eclipse.jetty.http.MetaData$Request")
                .interceptedBy("org.bithon.agent.plugin.httpserver.jetty12.interceptor.HttpChannelState$OnRequest")
                .build(),

            forClass("org.eclipse.jetty.server.internal.HttpChannelState$HandlerInvoker")
                .onMethod(Matchers.name("run").and(Matchers.argumentSize(0)))
                .interceptedBy("org.bithon.agent.plugin.httpserver.jetty12.interceptor.HandlerInvoker$Run")

                .onMethod(Matchers.name("completeStream").and(Matchers.takesArgument(1, "java.lang.Throwable")))
                .interceptedBy("org.bithon.agent.plugin.httpserver.jetty12.interceptor.HandlerInvoker$CompleteStream")
                .build()
        );
    }
}
