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

package org.bithon.agent.instrumentation.aop.interceptor.descriptor;

import org.bithon.agent.instrumentation.aop.interceptor.precondition.IInterceptorPrecondition;

/**
 * Class-oriented descriptor
 *
 * @author frank.chen021@outlook.com
 * @date 2021/2/21 12:50 上午
 */
public class InterceptorDescriptor {

    private final boolean debug;
    private final IInterceptorPrecondition precondition;
    private final String targetClass;
    private final MethodPointCutDescriptor[] methodPointCutDescriptors;

    public InterceptorDescriptor(boolean debug,
                                 IInterceptorPrecondition precondition,
                                 String targetClass,
                                 MethodPointCutDescriptor[] methodPointCutDescriptors) {
        this.debug = debug;
        this.precondition = precondition;
        this.targetClass = targetClass;
        this.methodPointCutDescriptors = methodPointCutDescriptors;
    }

    public String getTargetClass() {
        return targetClass;
    }

    public MethodPointCutDescriptor[] getMethodPointCutDescriptors() {
        return methodPointCutDescriptors;
    }

    public boolean isDebug() {
        return debug;
    }

    public IInterceptorPrecondition getPrecondition() {
        return precondition;
    }

    public InterceptorDescriptor withTargetClazz(String targetClass) {
        return new InterceptorDescriptor(debug, precondition, targetClass, methodPointCutDescriptors);
    }
}
