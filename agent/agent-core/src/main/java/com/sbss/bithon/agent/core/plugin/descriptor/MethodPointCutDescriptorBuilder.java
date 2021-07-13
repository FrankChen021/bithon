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

import shaded.net.bytebuddy.description.method.MethodDescription;
import shaded.net.bytebuddy.matcher.ElementMatcher;
import shaded.net.bytebuddy.matcher.ElementMatchers;

import static shaded.net.bytebuddy.matcher.ElementMatchers.isConstructor;
import static shaded.net.bytebuddy.matcher.ElementMatchers.named;
import static shaded.net.bytebuddy.matcher.ElementMatchers.takesNoArguments;

/**
 * @author frankchen
 * @date Jan 13, 2020 1:07:41 PM
 */
public class MethodPointCutDescriptorBuilder {

    private MethodType methodType;
    private ElementMatcher.Junction<MethodDescription> method;
    private ElementMatcher<MethodDescription> argsMatcher;
    private boolean debug;

    public static MethodPointCutDescriptorBuilder build() {
        return new MethodPointCutDescriptorBuilder();
    }

    public MethodPointCutDescriptor to(String interceptorQualifiedClassName) {

        ElementMatcher.Junction<? super MethodDescription> methodMatcher = MatcherUtils.debuggableMatcher(debug, method);
        if (argsMatcher != null) {
            methodMatcher = methodMatcher.and(argsMatcher);
        }
        return new MethodPointCutDescriptor(debug,
                                            methodMatcher,
                                            methodType,
                                            interceptorQualifiedClassName);
    }

    public MethodPointCutDescriptorBuilder onAllMethods(String method) {
        this.method = named(method);
        this.methodType = MethodType.NON_CONSTRUCTOR;
        return this;
    }

    public MethodPointCutDescriptorBuilder onMethodAndArgs(String method, String... args) {
        this.method = named(method);
        this.argsMatcher = MatcherUtils.createArgumentsMatcher(debug, args);
        this.methodType = MethodType.NON_CONSTRUCTOR;
        return this;
    }

    public MethodPointCutDescriptorBuilder onMethodAndNoArgs(String method) {
        this.method = named(method);
        this.argsMatcher = takesNoArguments();
        this.methodType = MethodType.NON_CONSTRUCTOR;
        return this;
    }

    public MethodPointCutDescriptorBuilder onMethod(ElementMatcher.Junction<MethodDescription> method) {
        this.method = method;
        this.methodType = MethodType.NON_CONSTRUCTOR;
        return this;
    }

    public MethodPointCutDescriptorBuilder onAllConstructor() {
        this.method = ElementMatchers.isConstructor();
        this.methodType = MethodType.CONSTRUCTOR;
        return this;
    }

    public MethodPointCutDescriptorBuilder onConstructor(ElementMatcher.Junction<MethodDescription> matcher) {
        this.method = isConstructor().and(matcher);
        this.methodType = MethodType.CONSTRUCTOR;
        return this;
    }

    public MethodPointCutDescriptorBuilder onConstructor(String... args) {
        if (args == null) {
            throw new IllegalArgumentException("args should not be null");
        }
        this.method = ElementMatchers.isConstructor();
        this.argsMatcher = MatcherUtils.createArgumentsMatcher(debug, args);
        this.methodType = MethodType.CONSTRUCTOR;
        return this;
    }

    public MethodPointCutDescriptorBuilder onDefaultConstructor() {
        this.method = ElementMatchers.isDefaultConstructor();
        this.methodType = MethodType.CONSTRUCTOR;
        return this;
    }

    public MethodPointCutDescriptorBuilder onArgs(String... args) {
        this.argsMatcher = MatcherUtils.createArgumentsMatcher(debug, args);
        return this;
    }

    public MethodPointCutDescriptorBuilder noArgs() {
        argsMatcher = takesNoArguments();
        return this;
    }

    public MethodPointCutDescriptorBuilder debug() {
        this.debug = true;
        return this;
    }
}
