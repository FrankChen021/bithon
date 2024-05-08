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

/**
 * @author frankchen
 * @date Jan 13, 2020 1:07:41 PM
 */
public class MethodPointCutDescriptorBuilder {

    private final InterceptorDescriptorBuilder interceptorDescriptorBuilder;
    private final MethodType methodType;
    private final ElementMatcher.Junction<MethodDescription> method;
    private ElementMatcher<MethodDescription> argsMatcher;
    private boolean debug;

    MethodPointCutDescriptorBuilder(InterceptorDescriptorBuilder interceptorDescriptorBuilder,
                                    ElementMatcher.Junction<MethodDescription> name,
                                    ElementMatcher<MethodDescription> argumentsMatcher,
                                    MethodType methodType) {
        this.interceptorDescriptorBuilder = interceptorDescriptorBuilder;
        this.method = name;
        this.argsMatcher = argumentsMatcher;
        this.methodType = methodType;
    }

    public MethodPointCutDescriptorBuilder andArgs(ElementMatcher<MethodDescription> matcher) {
        this.argsMatcher = matcher;
        return this;
    }

    public MethodPointCutDescriptorBuilder andNoArgs() {
        this.argsMatcher = Matchers.takesArguments(0);
        return this;
    }

    public MethodPointCutDescriptorBuilder andArgs(String... args) {
        this.argsMatcher = Matchers.createArgumentsMatcher(debug, args);
        return this;
    }

    public MethodPointCutDescriptorBuilder andRawArgs(String... args) {
        this.argsMatcher = Matchers.createArgumentsMatcher(debug, true, args);
        return this;
    }

    public InterceptorDescriptorBuilder interceptedBy(String interceptorQualifiedClassName) {
        if (method == null) {
            throw new AgentException("Failed to configure interceptor for 'method' has not been set.");
        }
        ElementMatcher.Junction<? super MethodDescription> methodMatcher = Matchers.debuggableMatcher(debug, method);
        if (argsMatcher != null) {
            methodMatcher = methodMatcher.and(argsMatcher);
        }
        interceptorDescriptorBuilder.add(new MethodPointCutDescriptor(debug,
                                                                      methodMatcher,
                                                                      methodType,
                                                                      interceptorQualifiedClassName));

        return interceptorDescriptorBuilder;
    }

    public InterceptorDescriptorBuilder replacedBy(String interceptorQualifiedClassName) {
        if (methodType == MethodType.CONSTRUCTOR) {
            throw new AgentException("Can't replace a constructor by [%s]", interceptorQualifiedClassName);
        }

        return interceptedBy(interceptorQualifiedClassName);
    }

    public MethodPointCutDescriptorBuilder debug() {
        this.debug = true;
        return this;
    }
}
