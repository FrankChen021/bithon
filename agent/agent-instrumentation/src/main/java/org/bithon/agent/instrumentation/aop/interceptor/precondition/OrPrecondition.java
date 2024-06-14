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

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/15
 */
class OrPrecondition implements IInterceptorPrecondition {
    private final IInterceptorPrecondition[] conditions;

    public OrPrecondition(IInterceptorPrecondition... conditions) {
        this.conditions = conditions;
    }

    @Override
    public boolean matches(ClassLoader classLoader, TypeDescription typeDescription) {
        for (IInterceptorPrecondition checker : conditions) {
            if (checker.matches(classLoader, typeDescription)) {
                return true;
            }
        }
        return false;
    }


    @Override
    public String toString() {
        return Stream.of(conditions).map((cond) -> "(" + cond.toString() + ")").collect(Collectors.joining(" OR "));
    }
}
