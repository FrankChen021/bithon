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

package org.bithon.agent.instrumentation.aop.interceptor.plugin;

import org.bithon.agent.instrumentation.aop.IBithonObject;
import org.bithon.agent.instrumentation.aop.interceptor.descriptor.BithonClassDescriptor;
import org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptor;
import org.bithon.agent.instrumentation.aop.interceptor.precondition.IInterceptorPrecondition;

import java.util.Collections;
import java.util.List;

/**
 * @author frankchen
 * @date 2020-12-31 22:29:55
 */
public interface IPlugin {

    default IInterceptorPrecondition getPreconditions() {
        return null;
    }

    /**
     * ALL classes in {@link #getInterceptors()} will be transformed as {@link IBithonObject} automatically.
     * But some classes needs to be transformed too to support passing objects especially those which provide ASYNC ability
     */
    default BithonClassDescriptor getBithonClassDescriptor() {
        return null;
    }

    /**
     * A list, each element of which is an interceptor for a specific method of class
     * NOTE, the target class will be instrumented as {@link IBithonObject}
     */
    default List<InterceptorDescriptor> getInterceptors() {
        return Collections.emptyList();
    }
}
