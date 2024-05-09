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
                .onMethod("process")
                .andArgsSize(4)
                .interceptedBy("org.bithon.agent.plugin.xxl.job.interceptor.EmbedHttpServerHandler$Process")
                .build(),

            // Inject the tracing context to the internal queue for tracing context restoring
            forClass("com.xxl.job.core.thread.JobThread")
                .onConstructor()
                .andArgs("int", "com.xxl.job.core.handler.IJobHandler")
                .interceptedBy("org.bithon.agent.plugin.xxl.job.interceptor.JobThread$Ctor")
                .build(),

            forClass("com.xxl.job.core.handler.impl.GlueJobHandler")
                .onMethod("execute")
                .andNoArgs()
                .interceptedBy("org.bithon.agent.plugin.xxl.job.interceptor.GlueJobHandler$Execute")
                .build(),

            forClass("com.xxl.job.core.handler.impl.MethodJobHandler")
                .onMethod("execute")
                .andNoArgs()
                .interceptedBy("org.bithon.agent.plugin.xxl.job.interceptor.MethodJobHandler$Execute")
                .build(),

            forClass("com.xxl.job.core.handler.impl.ScriptJobHandler")
                .onMethod("execute")
                .andNoArgs()
                .interceptedBy("org.bithon.agent.plugin.xxl.job.interceptor.ScriptJobHandler$Execute")
                .build()
        );
    }
}
