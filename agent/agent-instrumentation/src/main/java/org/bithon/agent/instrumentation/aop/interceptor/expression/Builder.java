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

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Interval;
import org.bithon.agent.instrumentation.aop.interceptor.InterceptorExpressionBaseVisitor;
import org.bithon.agent.instrumentation.aop.interceptor.InterceptorExpressionParser;
import org.bithon.agent.instrumentation.aop.interceptor.expression.matcher.ITypeMatcher;
import org.bithon.shaded.net.bytebuddy.description.method.MethodDescription;
import org.bithon.shaded.net.bytebuddy.matcher.ElementMatcher;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/3/26 22:12
 */
class Builder extends InterceptorExpressionBaseVisitor<Void> {
    private String clazzFilter;


    @Override
    public Void visitParse(InterceptorExpressionParser.ParseContext ctx) {
        InterceptorExpressionParser.WhenExpressionContext whenExpression = ctx.whenExpression();
        if (whenExpression != null) {

        }

        InterceptorExpressionParser.ModifierExpressionContext modifierExpression = ctx.modifierExpression();
        if (modifierExpression != null) {

        }

        InterceptorExpressionParser.ClassExpressionContext classExpression = ctx.classExpression();
        ITypeMatcher typeMatcher = classExpression.accept(new ClassExpressionVisitor());

        InterceptorExpressionParser.MethodExpressionContext methodExpression = ctx.methodExpression();
        ElementMatcher.Junction<MethodDescription> methodMatcher = methodExpression.accept(new MethodExpressionVisitor());
        return null;
    }


    private String getUnQuotedString(Token symbol) {
        CharStream input = symbol.getInputStream();
        if (input == null) {
            return null;
        } else {
            int n = input.size();
            int s = symbol.getStartIndex() + 1; // +1 to skip the leading quoted character
            int e = symbol.getStopIndex() - 1;  // -1 to skip the ending quoted character
            return s < n && e < n ? input.getText(Interval.of(s, e)) : "<EOF>";
        }
    }
}
