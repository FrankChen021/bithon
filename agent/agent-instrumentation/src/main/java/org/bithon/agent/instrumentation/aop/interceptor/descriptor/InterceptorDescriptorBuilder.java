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

import org.bithon.agent.instrumentation.aop.interceptor.matcher.Matchers;
import org.bithon.shaded.net.bytebuddy.description.method.MethodDescription;
import org.bithon.shaded.net.bytebuddy.matcher.ElementMatcher;
import org.bithon.shaded.net.bytebuddy.matcher.ElementMatchers;

import java.util.ArrayList;
import java.util.List;

/**
 * @author frankchen
 * @date Jan 13, 2020 1:07:41 PM
 */
public class InterceptorDescriptorBuilder {

    private final String targetClass;
    private boolean debug;
    private final List<MethodPointCutDescriptor> pointCuts = new ArrayList<>();

    public InterceptorDescriptorBuilder(String targetClass) {
        this.targetClass = targetClass;
    }

    public static InterceptorDescriptorBuilder forClass(String targetClass) {
        return new InterceptorDescriptorBuilder(targetClass);
    }

    public MethodPointCutDescriptorBuilder onMethod(String method) {
        return new MethodPointCutDescriptorBuilder(this,
                                                   Matchers.name(method),
                                                   MethodType.NON_CONSTRUCTOR);
    }

    public MethodPointCutDescriptorBuilder onMethod(ElementMatcher.Junction<MethodDescription> method) {
        return new MethodPointCutDescriptorBuilder(this,
                                                   method,
                                                   MethodType.NON_CONSTRUCTOR);
    }

    public MethodPointCutDescriptorBuilder onConstructor() {
        return new MethodPointCutDescriptorBuilder(this,
                                                   ElementMatchers.isConstructor(),
                                                   MethodType.CONSTRUCTOR);
    }

    public MethodPointCutDescriptorBuilder onDefaultConstructor() {
        return new MethodPointCutDescriptorBuilder(this,
                                                   ElementMatchers.isDefaultConstructor(),
                                                   MethodType.CONSTRUCTOR);
    }

    public InterceptorDescriptor build() {
        if (debug) {
            for (MethodPointCutDescriptor pointCut : pointCuts) {
                pointCut.setDebug(debug);
            }
        }
        return new InterceptorDescriptor(debug, targetClass, pointCuts.toArray(new MethodPointCutDescriptor[0]));
    }

    public InterceptorDescriptorBuilder debug() {
        this.debug = true;
        return this;
    }

    void add(MethodPointCutDescriptor methodPointCutDescriptor) {
        pointCuts.add(methodPointCutDescriptor);
    }
}
