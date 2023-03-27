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
import org.bithon.agent.instrumentation.aop.interceptor.expression.ConstExpression;
import org.bithon.agent.instrumentation.aop.interceptor.expression.FunctionCallExpression;
import org.bithon.agent.instrumentation.aop.interceptor.expression.matcher.ITypeMatcher;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/3/27 21:00
 */
public class ClassExpressionToMatcher extends InterceptorExpressionBaseVisitor<ITypeMatcher> {

    @Override
    public ITypeMatcher visitClassNameExpression(InterceptorExpressionParser.ClassNameExpressionContext ctx) {
        return new ITypeMatcher.NameMatcher(ctx.getText());
    }

    @Override
    public ITypeMatcher visitFunctionCallExpression(InterceptorExpressionParser.FunctionCallExpressionContext ctx) {
        FunctionCallExpression functionCallExpression = new FunctionCallExpression(ctx);
        if ("in".equals(functionCallExpression.getName())) {
            return buildInTypeMatcher(functionCallExpression);
        }

        throw new RuntimeException(String.format(Locale.ENGLISH, "Unsupported function [%s]", functionCallExpression.getName()));
    }

    private ITypeMatcher buildInTypeMatcher(FunctionCallExpression functionCallExpression) {
        List<ConstExpression> args = functionCallExpression.getArgs();
        if (args.isEmpty()) {
            throw new RuntimeException("Function 'in' must take at least in parameter.");
        }

        for (ConstExpression arg : args) {
            if (arg.getType() != InterceptorExpressionParser.STRING_LITERAL) {
                throw new RuntimeException(String.format(Locale.ENGLISH, "Argument %s must be type of STRING.", arg.getText()));
            }
        }

        if (args.size() == 1) {
            // An optimization
            return new ITypeMatcher.NameMatcher(args.get(0).getText());
        }

        return new ITypeMatcher.InMatcher(args.stream().map((arg) -> arg.getText()).collect(Collectors.toList()));
    }
}
