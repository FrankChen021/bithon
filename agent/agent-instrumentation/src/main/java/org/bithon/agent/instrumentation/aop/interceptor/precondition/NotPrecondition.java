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

package org.bithon.agent.instrumentation.aop.interceptor.precondition;

import org.bithon.shaded.net.bytebuddy.description.type.TypeDescription;

/**
 * @author frank.chen021@outlook.com
 * @date 2/5/24 4:20 pm
 */
class NotPrecondition implements IInterceptorPrecondition {
    private final IInterceptorPrecondition condition;

    public NotPrecondition(IInterceptorPrecondition condition) {
        this.condition = condition;
    }

    @Override
    public boolean matches(ClassLoader classLoader, TypeDescription typeDescription) {
        return !condition.matches(classLoader, typeDescription);
    }

    @Override
    public String toString() {
        return "NOT (" + condition.toString() + ")";
    }
}
