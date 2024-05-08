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

    private String targetClass;
    private boolean debug;
    private final List<MethodPointCutDescriptor> pointCuts = new ArrayList<>();

    public static InterceptorDescriptorBuilder forClass(String targetClass) {
        return new InterceptorDescriptorBuilder().targetClass(targetClass);
    }

    public MethodPointCutDescriptorBuilder onMethodName(String method) {
        return new MethodPointCutDescriptorBuilder(this,
                                                   Matchers.name(method),
                                                   null,
                                                   MethodType.NON_CONSTRUCTOR);
    }

    public MethodPointCutDescriptorBuilder onMethodAndArgs(String method, String... args) {
        return new MethodPointCutDescriptorBuilder(this,
                                                   Matchers.name(method),
                                                   Matchers.createArgumentsMatcher(debug, args),
                                                   MethodType.NON_CONSTRUCTOR);
    }

    public MethodPointCutDescriptorBuilder onMethodAndRawArgs(String method, String... args) {
        return new MethodPointCutDescriptorBuilder(this,
                                                   Matchers.name(method),
                                                   Matchers.createArgumentsMatcher(debug, true, args),
                                                   MethodType.NON_CONSTRUCTOR);
    }

    public MethodPointCutDescriptorBuilder onMethodAndNoArgs(String method) {
        return new MethodPointCutDescriptorBuilder(this,
                                                   Matchers.name(method),
                                                   ElementMatchers.takesNoArguments(),
                                                   MethodType.NON_CONSTRUCTOR);
    }

    public MethodPointCutDescriptorBuilder onMethod(ElementMatcher.Junction<MethodDescription> method) {
        return new MethodPointCutDescriptorBuilder(this,
                                                   method,
                                                   null,
                                                   MethodType.NON_CONSTRUCTOR);
    }

    public MethodPointCutDescriptorBuilder onAllConstructor() {
        return new MethodPointCutDescriptorBuilder(this,
                                                   ElementMatchers.isConstructor(),
                                                   null,
                                                   MethodType.CONSTRUCTOR);
    }

    public MethodPointCutDescriptorBuilder onConstructor(ElementMatcher.Junction<MethodDescription> matcher) {
        return new MethodPointCutDescriptorBuilder(this,
                                                   ElementMatchers.isConstructor().and(matcher),
                                                   null,
                                                   MethodType.CONSTRUCTOR);
    }

    public MethodPointCutDescriptorBuilder onConstructor(String... args) {
        if (args == null) {
            throw new IllegalArgumentException("args should not be null");
        }
        return new MethodPointCutDescriptorBuilder(this,
                                                   ElementMatchers.isConstructor(),
                                                   Matchers.createArgumentsMatcher(debug, args),
                                                   MethodType.CONSTRUCTOR);
    }

    public MethodPointCutDescriptorBuilder onDefaultConstructor() {
        return new MethodPointCutDescriptorBuilder(this,
                                                   ElementMatchers.isDefaultConstructor(),
                                                   null,
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

    public InterceptorDescriptorBuilder targetClass(String targetClass) {
        this.targetClass = targetClass;
        return this;
    }

    public InterceptorDescriptorBuilder debug() {
        this.debug = true;
        return this;
    }

    void add(MethodPointCutDescriptor methodPointCutDescriptor) {
        pointCuts.add(methodPointCutDescriptor);
    }
}
