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

package org.bithon.agent.instrumentation.aop.interceptor.expression;

import org.bithon.agent.instrumentation.aop.interceptor.expression.matcher.ITypeMatcher;
import org.bithon.shaded.net.bytebuddy.description.method.MethodDescription;
import org.bithon.shaded.net.bytebuddy.matcher.ElementMatcher;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/3/28 20:59
 */
public class ExpressionMatcher {
    private final ITypeMatcher clazzMatcher;
    private final ElementMatcher.Junction<? super MethodDescription> methodMatcher;
    private final boolean isCtor;

    public ExpressionMatcher(ITypeMatcher clazzMatcher,
                             ElementMatcher.Junction<? super MethodDescription> methodMatcher,
                             boolean ctor) {
        this.clazzMatcher = clazzMatcher;
        this.methodMatcher = methodMatcher;
        this.isCtor = ctor;
    }

    public ITypeMatcher getClazzMatcher() {
        return clazzMatcher;
    }

    public ElementMatcher.Junction<? super MethodDescription> getMethodMatcher() {
        return methodMatcher;
    }

    public boolean isCtor() {
        return isCtor;
    }
}
