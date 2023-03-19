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

package org.bithon.agent.core.interceptor.descriptor;

import org.bithon.agent.bootstrap.expt.AgentException;
import org.bithon.agent.core.interceptor.matcher.Matchers;
import org.bithon.shaded.net.bytebuddy.description.method.MethodDescription;
import org.bithon.shaded.net.bytebuddy.matcher.ElementMatcher;
import org.bithon.shaded.net.bytebuddy.matcher.ElementMatchers;

/**
 * @author frankchen
 * @date Jan 13, 2020 1:07:41 PM
 */
public class MethodPointCutDescriptorBuilder {

    private InterceptorType interceptorType;
    private ElementMatcher.Junction<MethodDescription> method;
    private ElementMatcher<MethodDescription> argsMatcher;
    private boolean debug;

    public static MethodPointCutDescriptorBuilder build() {
        return new MethodPointCutDescriptorBuilder();
    }

    public MethodPointCutDescriptor to(String interceptorQualifiedClassName) {
        if (method == null) {
            throw new AgentException("Failed to configure interceptor for 'method' has not been set.");
        }
        ElementMatcher.Junction<? super MethodDescription> methodMatcher = Matchers.debuggableMatcher(debug,
                                                                                                      method);
        if (argsMatcher != null) {
            methodMatcher = methodMatcher.and(argsMatcher);
        }
        return new MethodPointCutDescriptor(debug,
                                            methodMatcher,
                                            interceptorType,
                                            interceptorQualifiedClassName);
    }

    public MethodPointCutDescriptor replaceBy(String interceptorQualifiedClassName) {
        if (interceptorType == InterceptorType.CONSTRUCTOR) {
            throw new AgentException("Can't replace a constructor by [%s]", interceptorQualifiedClassName);
        }

        interceptorType = InterceptorType.REPLACEMENT;
        return to(interceptorQualifiedClassName);
    }

    public MethodPointCutDescriptorBuilder onAllMethods(String method) {
        this.method = Matchers.withName(method);
        this.interceptorType = InterceptorType.NON_CONSTRUCTOR;
        return this;
    }

    public MethodPointCutDescriptorBuilder onMethodAndArgs(String method, String... args) {
        this.method = Matchers.withName(method);
        this.argsMatcher = Matchers.createArgumentsMatcher(debug, args);
        this.interceptorType = InterceptorType.NON_CONSTRUCTOR;
        return this;
    }

    public MethodPointCutDescriptorBuilder onMethodAndRawArgs(String method, String... args) {
        this.method = Matchers.withName(method);
        this.argsMatcher = Matchers.createArgumentsMatcher(debug, true, args);
        this.interceptorType = InterceptorType.NON_CONSTRUCTOR;
        return this;
    }

    public MethodPointCutDescriptorBuilder onMethodAndNoArgs(String method) {
        this.method = Matchers.withName(method);
        this.argsMatcher = ElementMatchers.takesNoArguments();
        this.interceptorType = InterceptorType.NON_CONSTRUCTOR;
        return this;
    }

    public MethodPointCutDescriptorBuilder onMethod(ElementMatcher.Junction<MethodDescription> method) {
        this.method = method;
        this.interceptorType = InterceptorType.NON_CONSTRUCTOR;
        return this;
    }

    public MethodPointCutDescriptorBuilder onAllConstructor() {
        this.method = ElementMatchers.isConstructor();
        this.interceptorType = InterceptorType.CONSTRUCTOR;
        return this;
    }

    public MethodPointCutDescriptorBuilder onConstructor(ElementMatcher.Junction<MethodDescription> matcher) {
        this.method = ElementMatchers.isConstructor().and(matcher);
        this.interceptorType = InterceptorType.CONSTRUCTOR;
        return this;
    }

    public MethodPointCutDescriptorBuilder onConstructor(String... args) {
        if (args == null) {
            throw new IllegalArgumentException("args should not be null");
        }
        this.method = ElementMatchers.isConstructor();
        this.argsMatcher = Matchers.createArgumentsMatcher(debug, args);
        this.interceptorType = InterceptorType.CONSTRUCTOR;
        return this;
    }

    public MethodPointCutDescriptorBuilder onDefaultConstructor() {
        this.method = ElementMatchers.isDefaultConstructor();
        this.interceptorType = InterceptorType.CONSTRUCTOR;
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
