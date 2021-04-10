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

package com.sbss.bithon.agent.plugin.thread;

import com.sbss.bithon.agent.core.plugin.AbstractPlugin;
import com.sbss.bithon.agent.core.plugin.descriptor.InterceptorDescriptor;
import com.sbss.bithon.agent.core.plugin.descriptor.MethodPointCutDescriptorBuilder;

import java.util.Arrays;
import java.util.List;

import static com.sbss.bithon.agent.core.plugin.descriptor.InterceptorDescriptorBuilder.forBootstrapClass;

/**
 * @author frankchen
 */
public class ThreadPlugin extends AbstractPlugin {

    @Override
    public List<InterceptorDescriptor> getInterceptors() {

        return Arrays.asList(

            forBootstrapClass("java.util.concurrent.ThreadPoolExecutor")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onConstructor(
                                                       "int",
                                                       "int",
                                                       "long",
                                                       "java.util.concurrent.TimeUnit",
                                                       "java.util.concurrent.BlockingQueue<java.lang.Runnable>",
                                                       "java.util.concurrent.ThreadFactory",
                                                       "java.util.concurrent.RejectedExecutionHandler")
                                                   .to("com.sbss.bithon.agent.plugin.thread.threadpool.ThreadPoolExecutorConstructor"),

                    MethodPointCutDescriptorBuilder.build()
                                                   .onAllMethods("afterExecute")
                                                   .to("com.sbss.bithon.agent.plugin.thread.threadpool.ThreadPoolExecutorAfterExecute"),

                    MethodPointCutDescriptorBuilder.build()
                                                   .onAllMethods("shutdown")
                                                   .to("com.sbss.bithon.agent.plugin.thread.threadpool.ThreadPoolExecutorShutdown")
                ),

            forBootstrapClass("java.util.concurrent.ThreadPoolExecutor$AbortPolicy")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onAllMethods("rejectedExecution")
                                                   .to("com.sbss.bithon.agent.plugin.thread.threadpool.ThreadPoolExecutorAbort")
                ),

            forBootstrapClass("java.util.concurrent.ThreadPoolExecutor$CallerRunsPolicy")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onAllMethods("rejectedExecution")
                                                   .to("com.sbss.bithon.agent.plugin.thread.threadpool.ThreadPoolExecutorCallerRun")
                ),

            forBootstrapClass("java.util.concurrent.ThreadPoolExecutor$DiscardOldestPolicy")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onAllMethods("rejectedExecution")
                                                   .to("com.sbss.bithon.agent.plugin.thread.threadpool.ThreadPoolExecutorDiscardOldestPolicy")),

            forBootstrapClass("java.util.concurrent.ThreadPoolExecutor$DiscardOldestPolicy")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onAllMethods("rejectedExecution")
                                                   .to("com.sbss.bithon.agent.plugin.thread.threadpool.ThreadPoolExecutorDiscardPolicy")
                ),

            forBootstrapClass("java.util.concurrent.ForkJoinPool")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onConstructor("int",
                                                                  "java.util.concurrent.ForkJoinPool$ForkJoinWorkerThreadFactory",
                                                                  "java.lang.Thread$UncaughtExceptionHandler",
                                                                  "int",
                                                                  "java.lang.String")
                                                   .to("com.sbss.bithon.agent.plugin.thread.threadpool.ForkJoinPoolConstructor"),

                    MethodPointCutDescriptorBuilder.build()
                                                   .onAllMethods("tryTerminate")
                                                   .to("com.sbss.bithon.agent.plugin.thread.threadpool.ForkJoinPoolTryTerminate"),

                    MethodPointCutDescriptorBuilder.build()
                                                   .onAllMethods("externalPush")
                                                   .to("com.sbss.bithon.agent.plugin.thread.threadpool.ForkJoinPoolExternalPush")
                )
        );
    }
}


