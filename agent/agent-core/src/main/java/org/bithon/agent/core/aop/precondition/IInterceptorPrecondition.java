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

package org.bithon.agent.core.aop.precondition;

import shaded.net.bytebuddy.description.type.TypeDescription;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/17 8:13 下午
 */
public interface IInterceptorPrecondition {

    /**
     * Helper method
     */
    static IInterceptorPrecondition hasClass(String className) {
        return new HasClassPrecondition(className, false);
    }

    /**
     * Helper method
     */
    static IInterceptorPrecondition hasClass(String className, boolean debugging) {
        return new HasClassPrecondition(className, debugging);
    }

    static IInterceptorPrecondition or(IInterceptorPrecondition... conditions) {
        return new OrPrecondition(conditions);
    }

    /**
     * returns true if interceptors in this plugin can be installed
     *
     */
    boolean canInstall(String providerName, ClassLoader classLoader, TypeDescription typeDescription);
}
