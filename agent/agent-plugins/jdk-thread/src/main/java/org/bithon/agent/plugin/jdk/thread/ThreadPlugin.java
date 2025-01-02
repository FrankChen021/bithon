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
import org.bithon.agent.instrumentation.aop.interceptor.matcher.Matchers;
import org.bithon.agent.instrumentation.aop.interceptor.plugin.IPlugin;
import org.bithon.shaded.net.bytebuddy.description.modifier.Visibility;

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
                .onConstructor()
                .andArgs("int",
                         "int",
                         "long",
                         "java.util.concurrent.TimeUnit",
                         "java.util.concurrent.BlockingQueue<java.lang.Runnable>",
                         "java.util.concurrent.ThreadFactory",
                         "java.util.concurrent.RejectedExecutionHandler")
                .interceptedBy("org.bithon.agent.plugin.jdk.thread.interceptor.ThreadPoolExecutor$Ctor")

                .onMethod("execute")
                .andArgs("java.lang.Runnable")
                .interceptedBy("org.bithon.agent.plugin.jdk.thread.interceptor.ThreadPoolExecutor$Execute")

                .onMethod("remove")
                .andArgs("java.lang.Runnable")
                .interceptedBy("org.bithon.agent.plugin.jdk.thread.interceptor.ThreadPoolExecutor$Remove")

                .onMethod("shutdown")
                .interceptedBy("org.bithon.agent.plugin.jdk.thread.interceptor.ThreadPoolExecutor$Shutdown")
                .build(),

            forClass("java.util.concurrent.ForkJoinPool")
                // Interceptors on ctor are defined in jdk8-thread and jdk9-thread plugins

                .onMethod("tryTerminate")
                .interceptedBy("org.bithon.agent.plugin.jdk.thread.interceptor.ForkJoinPool$TryTerminate")

                .onMethod("externalPush")
                .interceptedBy("org.bithon.agent.plugin.jdk.thread.interceptor.ForkJoinPool$ExternalPush")
                .build(),

            forClass("java.util.concurrent.ForkJoinTask$AdaptedCallable")
                .onConstructor()
                .debug()
                .interceptedBy("org.bithon.agent.plugin.jdk.thread.interceptor.ForkJoinTaskAdaptedCallable$Ctor")
                .build(),

            forClass("java.util.concurrent.ForkJoinTask$AdaptedInterruptibleCallable")
                .onConstructor()
                .debug()
                .interceptedBy("org.bithon.agent.plugin.jdk.thread.interceptor.ForkJoinTaskAdaptedInterruptibleCallable$Ctor")
                .build(),

            forClass("java.util.concurrent.ForkJoinTask$AdaptedRunnable")
                .onConstructor()
                .debug()
                .interceptedBy("org.bithon.agent.plugin.jdk.thread.interceptor.ForkJoinTaskAdaptedRunnable$Ctor")
                .build(),

            forClass("java.util.concurrent.ForkjoinTask$AdaptedRunnableAction")
                .onConstructor()
                .debug()
                .interceptedBy("org.bithon.agent.plugin.jdk.thread.interceptor.ForkJoinTaskAdaptedRunnableAction$Ctor")
                .build(),

            forClass("java.util.concurrent.ForkJoinTask$RunnableExecuteAction")
                .onConstructor()
                .debug()
                .interceptedBy("org.bithon.agent.plugin.jdk.thread.interceptor.ForkJoinTaskRunnableExecuteAction$Ctor")
                .build(),

            forClass("java.util.concurrent.ForkJoinTask")
                .onConstructor().andArgsSize(0).andVisibility(Visibility.PUBLIC)
                .interceptedBy("org.bithon.agent.plugin.jdk.thread.interceptor.ForkJoinTask$Ctor")

                .onMethod(Matchers.name("doExec").and(Matchers.argumentSize(0)))
                .interceptedBy("org.bithon.agent.plugin.jdk.thread.interceptor.ForkJoinTask$DoExec")
                .build()
        );
    }
}


