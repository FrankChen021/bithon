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

package org.bithon.agent.plugin.quartz2;

import org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptor;
import org.bithon.agent.instrumentation.aop.interceptor.plugin.IPlugin;
import org.bithon.agent.instrumentation.aop.interceptor.precondition.IInterceptorPrecondition;

import java.util.Collections;
import java.util.List;

import static org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptorBuilder.forClass;

/**
 * @author frankchen
 */
public class QuartzPlugin implements IPlugin {

    @Override
    public IInterceptorPrecondition getPreconditions() {
        return IInterceptorPrecondition.isClassDefined("org.quartz.JobKey");
    }

    @Override
    public List<InterceptorDescriptor> getInterceptors() {

        return Collections.singletonList(
            forClass("org.quartz.core.JobRunShell")
                .onMethodAndNoArgs("run")
                .interceptedBy("org.bithon.agent.plugin.quartz2.JobRunShell$Run")

                .onMethodName("notifyJobListenersComplete")
                .interceptedBy("org.bithon.agent.plugin.quartz2.JobRunShell$NotifyJobListenersComplete")
                .build()
        );
    }
}
