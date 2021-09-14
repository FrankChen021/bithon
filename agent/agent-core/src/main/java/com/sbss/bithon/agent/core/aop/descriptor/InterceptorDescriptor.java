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

package com.sbss.bithon.agent.core.aop.descriptor;

import shaded.net.bytebuddy.description.type.TypeDescription;
import shaded.net.bytebuddy.matcher.ElementMatcher;

/**
 * Class-oriented descriptor
 *
 * @author frank.chen021@outlook.com
 * @date 2021/2/21 12:50 上午
 */
public class InterceptorDescriptor {

    private final boolean debug;
    private final boolean isBootstrapClass;
    private final ElementMatcher.Junction<? super TypeDescription> classMatcher;
    private final MethodPointCutDescriptor[] methodPointCutDescriptors;

    public InterceptorDescriptor(boolean debug,
                                 boolean isBootstrapClass,
                                 ElementMatcher.Junction<? super TypeDescription> classMatcher,
                                 MethodPointCutDescriptor[] methodPointCutDescriptors) {
        this.debug = debug;
        this.isBootstrapClass = isBootstrapClass;
        this.classMatcher = classMatcher;
        this.methodPointCutDescriptors = methodPointCutDescriptors;
    }

    public boolean isBootstrapClass() {
        return isBootstrapClass;
    }

    public ElementMatcher.Junction<? super TypeDescription> getClassMatcher() {
        return classMatcher;
    }

    public MethodPointCutDescriptor[] getMethodPointCutDescriptors() {
        return methodPointCutDescriptors;
    }

    public boolean isDebug() {
        return debug;
    }
}
