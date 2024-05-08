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
import org.bithon.agent.instrumentation.expt.AgentException;
import org.bithon.shaded.net.bytebuddy.description.method.MethodDescription;
import org.bithon.shaded.net.bytebuddy.matcher.ElementMatcher;
import org.bithon.shaded.net.bytebuddy.matcher.ElementMatchers;

/**
 * @author frankchen
 * @date Jan 13, 2020 1:07:41 PM
 */
public class MethodPointCutDescriptorBuilder {

    private final InterceptorDescriptorBuilder interceptorDescriptorBuilder;
    private MethodType methodType;
    private ElementMatcher.Junction<MethodDescription> method;
    private ElementMatcher<MethodDescription> argsMatcher;
    private boolean debug;

    MethodPointCutDescriptorBuilder(InterceptorDescriptorBuilder interceptorDescriptorBuilder) {
        this.interceptorDescriptorBuilder = interceptorDescriptorBuilder;
    }

    public InterceptorDescriptorBuilder to(String interceptorQualifiedClassName) {
        if (method == null) {
            throw new AgentException("Failed to configure interceptor for 'method' has not been set.");
        }
        ElementMatcher.Junction<? super MethodDescription> methodMatcher = Matchers.debuggableMatcher(debug,
                                                                                                      method);
        if (argsMatcher != null) {
            methodMatcher = methodMatcher.and(argsMatcher);
        }
        interceptorDescriptorBuilder.add(new MethodPointCutDescriptor(debug,
                                            methodMatcher,
                                            methodType,
                                            interceptorQualifiedClassName));

        return interceptorDescriptorBuilder;
    }

    public InterceptorDescriptorBuilder replaceBy(String interceptorQualifiedClassName) {
        if (methodType == MethodType.CONSTRUCTOR) {
            throw new AgentException("Can't replace a constructor by [%s]", interceptorQualifiedClassName);
        }

        return to(interceptorQualifiedClassName);
    }

    public MethodPointCutDescriptorBuilder onMethodName(String method) {
        this.method = Matchers.name(method);
        this.methodType = MethodType.NON_CONSTRUCTOR;
        return this;
    }

    public MethodPointCutDescriptorBuilder onMethodAndArgs(String method, String... args) {
        this.method = Matchers.name(method);
        this.argsMatcher = Matchers.createArgumentsMatcher(debug, args);
        this.methodType = MethodType.NON_CONSTRUCTOR;
        return this;
    }

    public MethodPointCutDescriptorBuilder onMethodAndRawArgs(String method, String... args) {
        this.method = Matchers.name(method);
        this.argsMatcher = Matchers.createArgumentsMatcher(debug, true, args);
        this.methodType = MethodType.NON_CONSTRUCTOR;
        return this;
    }

    public MethodPointCutDescriptorBuilder onMethodAndNoArgs(String method) {
        this.method = Matchers.name(method);
        this.argsMatcher = ElementMatchers.takesNoArguments();
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
        this.method = ElementMatchers.isConstructor().and(matcher);
        this.methodType = MethodType.CONSTRUCTOR;
        return this;
    }

    public MethodPointCutDescriptorBuilder onConstructor(String... args) {
        if (args == null) {
            throw new IllegalArgumentException("args should not be null");
        }
        this.method = ElementMatchers.isConstructor();
        this.argsMatcher = Matchers.createArgumentsMatcher(debug, args);
        this.methodType = MethodType.CONSTRUCTOR;
        return this;
    }

    public MethodPointCutDescriptorBuilder onDefaultConstructor() {
        this.method = ElementMatchers.isDefaultConstructor();
        this.methodType = MethodType.CONSTRUCTOR;
        return this;
    }

    public MethodPointCutDescriptorBuilder onArgs(String... args) {
        this.argsMatcher = Matchers.createArgumentsMatcher(debug, args);
        return this;
    }

    public MethodPointCutDescriptorBuilder noArgs() {
        argsMatcher = ElementMatchers.takesNoArguments();
        return this;
    }

    public MethodPointCutDescriptorBuilder debug() {
        this.debug = true;
        return this;
    }
}
