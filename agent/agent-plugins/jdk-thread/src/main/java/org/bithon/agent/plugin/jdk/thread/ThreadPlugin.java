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

package org.bithon.agent.plugin.jdk.thread;

import org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptor;
import org.bithon.agent.instrumentation.aop.interceptor.plugin.IPlugin;

import java.util.Arrays;
import java.util.List;

import static org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptorBuilder.forClass;

/**
 * @author frankchen
 */
public class ThreadPlugin implements IPlugin {

    @Override
    public List<InterceptorDescriptor> getInterceptors() {
        return Arrays.asList(
            forClass("java.util.concurrent.ThreadPoolExecutor")
                .onConstructor(
                    "int",
                    "int",
                    "long",
                    "java.util.concurrent.TimeUnit",
                    "java.util.concurrent.BlockingQueue<java.lang.Runnable>",
                    "java.util.concurrent.ThreadFactory",
                    "java.util.concurrent.RejectedExecutionHandler")
                .interceptedBy("org.bithon.agent.plugin.jdk.thread.interceptor.ThreadPoolExecutor$Ctor")

                .onMethodAndArgs("execute", "java.lang.Runnable")
                .interceptedBy("org.bithon.agent.plugin.jdk.thread.interceptor.ThreadPoolExecutor$Execute")

                .onMethodAndArgs("remove", "java.lang.Runnable")
                .interceptedBy("org.bithon.agent.plugin.jdk.thread.interceptor.ThreadPoolExecutor$Remove")

                .onMethodName("shutdown")
                .interceptedBy("org.bithon.agent.plugin.jdk.thread.interceptor.ThreadPoolExecutor$Shutdown")
                .build(),

            forClass("java.util.concurrent.ForkJoinPool")
                .onConstructor("int",
                               "java.util.concurrent.ForkJoinPool$ForkJoinWorkerThreadFactory",
                               "java.lang.Thread$UncaughtExceptionHandler",
                               "int",
                               "java.lang.String")
                .interceptedBy("org.bithon.agent.plugin.jdk.thread.interceptor.ForkJoinPool$Ctor")

                .onMethodName("tryTerminate")
                .interceptedBy("org.bithon.agent.plugin.jdk.thread.interceptor.ForkJoinPool$TryTerminate")

                .onMethodName("externalPush")
                .interceptedBy("org.bithon.agent.plugin.jdk.thread.interceptor.ForkJoinPool$ExternalPush")
                .build()
        );
    }
}


