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
import org.bithon.shaded.net.bytebuddy.description.modifier.Visibility;
import org.bithon.shaded.net.bytebuddy.matcher.ElementMatcher;

import java.util.function.Function;

/**
 * @author frankchen
 * @date Jan 13, 2020 1:07:41 PM
 */
public class MethodPointCutDescriptorBuilder {

    private final InterceptorDescriptorBuilder interceptorDescriptorBuilder;
    private final MethodType methodType;
    private final ElementMatcher.Junction<MethodDescription> method;
    private ElementMatcher.Junction<MethodDescription> argsMatcher;
    private boolean debug;
    private Visibility[] visibility;

    MethodPointCutDescriptorBuilder(InterceptorDescriptorBuilder interceptorDescriptorBuilder,
                                    ElementMatcher.Junction<MethodDescription> name,
                                    MethodType methodType) {
        this.interceptorDescriptorBuilder = interceptorDescriptorBuilder;
        this.method = name;
        this.methodType = methodType;
    }

    public MethodPointCutDescriptorBuilder andArgs(ElementMatcher.Junction<MethodDescription> matcher) {
        setArgsMatcher(matcher);
        return this;
    }

    public MethodPointCutDescriptorBuilder andNoArgs() {
        setArgsMatcher(Matchers.argumentSize(0));
        return this;
    }

    public MethodPointCutDescriptorBuilder andArgsSize(int size) {
        setArgsMatcher(Matchers.argumentSize(size));
        return this;
    }

    public MethodPointCutDescriptorBuilder andArgsSize(Function<Integer, Boolean> comparator) {
        setArgsMatcher(new ElementMatcher.Junction.AbstractBase<MethodDescription>() {

            private final Function<Integer, Boolean> sizeComparator = comparator;

            @Override
            public boolean matches(MethodDescription target) {
                return sizeComparator.apply(target.getParameters().size());
            }
        });
        return this;
    }

    public MethodPointCutDescriptorBuilder andArgs(String... args) {
        setArgsMatcher(Matchers.createArgumentsMatcher(debug, args));
        return this;
    }

    public MethodPointCutDescriptorBuilder andRawArgs(String... args) {
        setArgsMatcher(Matchers.createArgumentsMatcher(debug, true, args));
        return this;
    }

    public MethodPointCutDescriptorBuilder andArgs(int index, String typeName) {
        setArgsMatcher(Matchers.takesArgument(index, typeName));
        return this;
    }

    public MethodPointCutDescriptorBuilder andVisibility(Visibility... visibility) {
        this.visibility = visibility;
        return this;
    }

    public InterceptorDescriptorBuilder interceptedBy(String interceptorQualifiedClassName) {
        if (method == null) {
            throw new AgentException("Failed to configure interceptor for 'method' has not been set.");
        }
        ElementMatcher.Junction<? super MethodDescription> methodMatcher = Matchers.debuggableMatcher(debug, method);
        if (visibility != null) {
            methodMatcher = methodMatcher.and(Matchers.visibility(visibility));
        }
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

    private void setArgsMatcher(ElementMatcher.Junction<MethodDescription> matcher) {
        this.argsMatcher = this.argsMatcher == null ? matcher : this.argsMatcher.and(matcher);
    }
}
