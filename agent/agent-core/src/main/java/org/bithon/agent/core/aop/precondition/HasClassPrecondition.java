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

package org.bithon.agent.core.aop.precondition;

import shaded.net.bytebuddy.description.type.TypeDescription;
import shaded.org.slf4j.LoggerFactory;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/17 8:14 下午
 */
class HasClassPrecondition implements IInterceptorPrecondition {

    private final String className;
    private final boolean debugging;

    public HasClassPrecondition(String className, boolean debugging) {
        this.className = className;
        this.debugging = debugging;
    }

    @Override
    public boolean canInstall(String providerName,
                              ClassLoader classLoader,
                              TypeDescription typeDescription) {
        boolean resolved = TypeResolver.getInstance().isResolved(classLoader, this.className);
        if (!resolved && this.debugging) {
            LoggerFactory.getLogger(HasClassPrecondition.class)
                         .info("Required class [{}] not found to install interceptor for [{}] in [{}]",
                               this.className,
                               typeDescription.getName(),
                               providerName);
        }
        return resolved;
    }
}
