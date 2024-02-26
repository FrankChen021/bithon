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

package org.bithon.agent.plugin.xxl.job;

import org.bithon.agent.instrumentation.aop.interceptor.descriptor.BithonClassDescriptor;
import org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptor;
import org.bithon.agent.instrumentation.aop.interceptor.descriptor.MethodPointCutDescriptorBuilder;
import org.bithon.agent.instrumentation.aop.interceptor.matcher.Matchers;
import org.bithon.agent.instrumentation.aop.interceptor.plugin.IPlugin;

import java.util.Arrays;
import java.util.List;

import static org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptorBuilder.forClass;

/**
 * @author frankchen
 */
public class XxlJobPlugin implements IPlugin {

    @Override
    public BithonClassDescriptor getBithonClassDescriptor() {
        // Enhance the TriggerParam to hold context so that the job scheduling can restore the tracing context
        return BithonClassDescriptor.of("com.xxl.job.core.biz.model.TriggerParam");
    }

    @Override
    public List<InterceptorDescriptor> getInterceptors() {

        return Arrays.asList(
            forClass("com.xxl.job.core.server.EmbedServer$EmbedHttpServerHandler")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethod(Matchers.withName("process").and(Matchers.takesArguments(4)))
                                                   .to("org.bithon.agent.plugin.xxl.job.interceptor.EmbedHttpServerHandler$Process")
                        ),

            // Inject the tracing context to the internal queue for tracing context restoring
            forClass("com.xxl.job.core.thread.JobThread")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onConstructor("int", "com.xxl.job.core.handler.IJobHandler")
                                                   .to("org.bithon.agent.plugin.xxl.job.interceptor.JobThread$Ctor")
                        ),

            forClass("com.xxl.job.core.handler.impl.GlueJobHandler")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndNoArgs("execute")
                                                   .to("org.bithon.agent.plugin.xxl.job.interceptor.GlueJobHandler$Execute")
                        ),

            forClass("com.xxl.job.core.handler.impl.MethodJobHandler")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndNoArgs("execute")
                                                   .to("org.bithon.agent.plugin.xxl.job.interceptor.MethodJobHandler$Execute")
                        ),

            forClass("com.xxl.job.core.handler.impl.ScriptJobHandler")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndNoArgs("execute")
                                                   .to("org.bithon.agent.plugin.xxl.job.interceptor.ScriptJobHandler$Execute")
                        ));
    }
}
