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

package com.sbss.bithon.agent.core.plugin.descriptor;

import shaded.net.bytebuddy.description.type.TypeDescription;
import shaded.net.bytebuddy.matcher.ElementMatcher;

import static shaded.net.bytebuddy.matcher.ElementMatchers.named;

/**
 * @author frankchen
 * @date Jan 13, 2020 1:07:41 PM
 */
public class InterceptorDescriptorBuilder {

    private ElementMatcher.Junction<? super TypeDescription> targetClass;
    private boolean debug;
    private boolean isBootstrapClass;

    public static InterceptorDescriptorBuilder forClass(String targetClass) {
        return forClass(named(targetClass));
    }

    public static InterceptorDescriptorBuilder forClass(ElementMatcher.Junction<? super TypeDescription> targetClassMatcher) {
        return new InterceptorDescriptorBuilder().targetClass(targetClassMatcher);
    }

    /**
     * for classes which are loaded by bootstrap class loader
     */
    public static InterceptorDescriptorBuilder forBootstrapClass(String targetClass) {
        return new InterceptorDescriptorBuilder().targetClass(targetClass).isBootstrapClass(true);
    }

    public InterceptorDescriptor methods(MethodPointCutDescriptor... pointCuts) {
        if (debug) {
            for (MethodPointCutDescriptor pointCut : pointCuts) {
                pointCut.setDebug(debug);
            }
        }
        return new InterceptorDescriptor(debug, isBootstrapClass, targetClass, pointCuts);

    }

    public InterceptorDescriptorBuilder targetClass(ElementMatcher.Junction<? super TypeDescription> classMatcher) {
        this.targetClass = classMatcher;
        return this;
    }

    public InterceptorDescriptorBuilder targetClass(String targetClass) {
        this.targetClass = named(targetClass);
        return this;
    }

    public InterceptorDescriptorBuilder debug() {
        this.debug = true;
        return this;
    }

    public InterceptorDescriptorBuilder isBootstrapClass(boolean isBootstrapClass) {
        this.isBootstrapClass = isBootstrapClass;
        return this;
    }
}
