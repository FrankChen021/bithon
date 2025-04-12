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

package org.bithon.agent.plugin.thread.jdk21;

import org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptor;
import org.bithon.agent.instrumentation.aop.interceptor.plugin.IPlugin;
import org.bithon.agent.instrumentation.aop.interceptor.precondition.IInterceptorPrecondition;
import org.bithon.component.commons.utils.JdkUtils;
import org.bithon.shaded.net.bytebuddy.description.type.TypeDescription;

import java.util.Collections;
import java.util.List;

import static org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptorBuilder.forClass;

/**
 * @author frankchen
 */
public class ThreadJdk21Plugin implements IPlugin {

    @Override
    public List<InterceptorDescriptor> getInterceptors() {
        return Collections.singletonList(
            forClass("java.util.concurrent.ForkJoinPool")
                .whenSatisfy(new IInterceptorPrecondition() {
                    @Override
                    public boolean matches(ClassLoader classLoader, TypeDescription typeDescription) {
                        return JdkUtils.getMajorVersion() >= 21;
                    }

                    @Override
                    public String toString() {
                        return "Jdk >= 21";
                    }
                })

                .onMethod("poolSubmit")
                .debug()
                .andArgs(1, "java.util.concurrent.ForkJoinTask")
                .interceptedBy("org.bithon.agent.plugin.thread.jdk21.ForkJoinPool$PoolSubmit")
                .build()
        );
    }
}
