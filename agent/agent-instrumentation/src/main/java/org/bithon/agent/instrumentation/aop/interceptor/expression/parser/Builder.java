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

package org.bithon.agent.instrumentation.aop.interceptor.expression.parser;

import org.bithon.agent.instrumentation.aop.interceptor.InterceptorExpressionBaseVisitor;
import org.bithon.agent.instrumentation.aop.interceptor.InterceptorExpressionParser;
import org.bithon.agent.instrumentation.aop.interceptor.expression.ExpressionMatcher;
import org.bithon.agent.instrumentation.aop.interceptor.expression.matcher.ITypeMatcher;
import org.bithon.shaded.net.bytebuddy.description.method.MethodDescription;
import org.bithon.shaded.net.bytebuddy.matcher.ElementMatcher;
import org.bithon.shaded.net.bytebuddy.matcher.ElementMatchers;

import java.util.Locale;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/3/26 22:12
 */
class Builder extends InterceptorExpressionBaseVisitor<ExpressionMatcher> {

    @Override
    public ExpressionMatcher visitParse(InterceptorExpressionParser.ParseContext ctx) {
        InterceptorExpressionParser.WhenExpressionContext whenExpression = ctx.whenExpression();
        if (whenExpression != null) {

        }

        ElementMatcher.Junction<? super MethodDescription> modifierMatcher = null;
        InterceptorExpressionParser.ModifierExpressionContext modifierExpression = ctx.modifierExpression();
        if (modifierExpression != null) {
            String methodModifier = modifierExpression.getText();
            switch (methodModifier) {
                case "public":
                    modifierMatcher = ElementMatchers.isPublic();
                    break;
                case "private":
                    modifierMatcher = ElementMatchers.isPrivate();
                    break;
                case "protected":
                    modifierMatcher = ElementMatchers.isProtected();
                    break;
                default:
                    throw new RuntimeException(String.format(Locale.ENGLISH, "Not supported modifier [%s]", methodModifier));
            }
        }

        InterceptorExpressionParser.ClassExpressionContext classExpression = ctx.classExpression();
        ITypeMatcher clazzMatcher = classExpression.accept(new ClassExpressionToMatcher());

        InterceptorExpressionParser.MethodExpressionContext methodExpression = ctx.methodExpression();
        ElementMatcher.Junction<? super MethodDescription> methodMatcher = methodExpression.accept(new MethodExpressionToMatcher());
        if (modifierMatcher != null) {
            methodMatcher = modifierMatcher.and(methodMatcher);
        }

        return new ExpressionMatcher(clazzMatcher, methodMatcher);
    }
}
