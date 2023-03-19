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

import org.bithon.agent.bootstrap.aop.interceptor.InterceptorType;
import org.bithon.shaded.net.bytebuddy.description.method.MethodDescription;
import org.bithon.shaded.net.bytebuddy.matcher.ElementMatcher;

/**
 * @author frankchen
 * @date Jan 13, 2020 1:07:41 PM
 */
public class MethodPointCutDescriptor {

    private boolean debug;
    private final ElementMatcher.Junction<? super MethodDescription> methodMatcher;
    private InterceptorType interceptorType;
    private final MethodType methodType;
    private final String interceptorClassName;

    public MethodPointCutDescriptor(boolean debug,
                                    ElementMatcher.Junction<? super MethodDescription> methodMatcher,
                                    MethodType methodType,
                                    String interceptorClassName) {
        this.debug = debug;
        this.methodMatcher = methodMatcher;
        this.methodType = methodType;
        this.interceptorClassName = interceptorClassName;
    }

    public ElementMatcher.Junction<? super MethodDescription> getMethodMatcher() {
        return methodMatcher;
    }

    public MethodType getMethodType() {
        return methodType;
    }

    public InterceptorType getInterceptorType() {
        return interceptorType;
    }

    public void setInterceptorType(InterceptorType interceptorType) {
        this.interceptorType = interceptorType;
    }

    public boolean isDebug() {
        return debug;
    }

    public String getInterceptorClassName() {
        return interceptorClassName;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    @Override
    public String toString() {
        return methodMatcher.toString();
    }
}
