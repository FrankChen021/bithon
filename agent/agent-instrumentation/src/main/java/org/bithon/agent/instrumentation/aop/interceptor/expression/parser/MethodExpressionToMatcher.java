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

import org.antlr.v4.runtime.tree.ParseTree;
import org.bithon.agent.instrumentation.aop.interceptor.InterceptorExpressionBaseVisitor;
import org.bithon.agent.instrumentation.aop.interceptor.InterceptorExpressionParser;
import org.bithon.agent.instrumentation.aop.interceptor.expression.FunctionCallExpression;
import org.bithon.agent.instrumentation.aop.interceptor.expression.matcher.And;
import org.bithon.agent.instrumentation.aop.interceptor.expression.matcher.IArgumentMatcher;
import org.bithon.agent.instrumentation.aop.interceptor.expression.matcher.Or;
import org.bithon.agent.instrumentation.aop.interceptor.matcher.Matchers;
import org.bithon.shaded.net.bytebuddy.description.method.MethodDescription;
import org.bithon.shaded.net.bytebuddy.matcher.ElementMatcher;
import org.bithon.shaded.net.bytebuddy.matcher.ElementMatchers;
import org.bithon.shaded.net.bytebuddy.matcher.NameMatcher;
import org.bithon.shaded.net.bytebuddy.matcher.StringSetMatcher;

import java.util.Locale;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/3/27 21:13
 */
public class MethodExpressionToMatcher extends InterceptorExpressionBaseVisitor<ElementMatcher.Junction<? super MethodDescription>> {

    private ElementMatcher.Junction<? super MethodDescription> matcher;


    @Override
    public ElementMatcher.Junction<? super MethodDescription> visitMethodExpression(InterceptorExpressionParser.MethodExpressionContext ctx) {
        super.visitMethodExpression(ctx);
        return matcher;
    }

    @Override
    public ElementMatcher.Junction<? super MethodDescription> visitMethodNameExpression(InterceptorExpressionParser.MethodNameExpressionContext ctx) {
        String methodName = ctx.getText();
        matcher = ElementMatchers.named(methodName);
        return null;
    }

    @Override
    public ElementMatcher.Junction<? super MethodDescription> visitMethodFunctionExpression(InterceptorExpressionParser.MethodFunctionExpressionContext ctx) {
        FunctionCallExpression functionCallExpression = new FunctionCallExpression(ctx.functionCallExpression());
        switch (functionCallExpression.getName()) {
            case "annotated":
                matcher = ElementMatchers.isAnnotatedWith(new NameMatcher<>(new StringSetMatcher(functionCallExpression.getArgs()
                                                                                                                       .stream()
                                                                                                                       .map(arg -> arg.getText())
                                                                                                                       .collect(Collectors.toSet()))));
                break;
            case "overridden":
                matcher = ElementMatchers.isOverriddenFrom(new NameMatcher<>(new StringSetMatcher(functionCallExpression.getArgs()
                                                                                                                        .stream()
                                                                                                                        .map(arg -> arg.getText())
                                                                                                                        .collect(Collectors.toSet()))));
                break;
            default:
                throw new RuntimeException("Not supported function " + functionCallExpression.getName());
        }
        return null;
    }

    @Override
    public ElementMatcher.Junction<? super MethodDescription> visitMethodArgExpression(InterceptorExpressionParser.MethodArgExpressionContext ctx) {
        if (ctx.children.size() == 2) {
            // No argument given
            return null;
        }

        // 3 children for this expression
        if (ctx.children.get(1) instanceof InterceptorExpressionParser.ArgFilterExpressionContext) {
            ctx.getChild(1).accept(new ArgumentMatcherBuilder());
        } else {
            int argumentSize = Integer.parseInt(ctx.children.get(1).getText());
            matcher = matcher.and(Matchers.takesArguments(argumentSize));
        }

        return null;
    }

    static class ArgumentMatcherBuilder extends InterceptorExpressionBaseVisitor<ElementMatcher<? super MethodDescription>> {

        // Nested filter
        @Override
        public ElementMatcher<? super MethodDescription> visitArgFilterExpression(InterceptorExpressionParser.ArgFilterExpressionContext ctx) {
            if (ctx.children.size() == 1) {
                // binary Expression
                return ctx.children.get(0).accept(this);
            } else {
                // argFilterExpression logicExpression argFilterExpression
                InterceptorExpressionParser.ArgFilterExpressionContext left = (InterceptorExpressionParser.ArgFilterExpressionContext) ctx.getChild(0);
                InterceptorExpressionParser.LogicExpressionContext logicExpression = (InterceptorExpressionParser.LogicExpressionContext) ctx.getChild(1);
                InterceptorExpressionParser.ArgFilterExpressionContext right = (InterceptorExpressionParser.ArgFilterExpressionContext) ctx.getChild(2);

                String logicOperator = logicExpression.getText().toUpperCase(Locale.ROOT);
                if ("AND".equals(logicOperator)) {
                    return new And(left.accept(this), right.accept(this));
                }
                if ("OR".equals(logicOperator)) {
                    return new Or(left.accept(this), right.accept(this));
                }
                throw new RuntimeException("Unsupported operator " + logicOperator);
            }
        }

        @Override
        public ElementMatcher<? super MethodDescription> visitBinaryExpression(InterceptorExpressionParser.BinaryExpressionContext ctx) {
            InterceptorExpressionParser.ObjectExpressionContext left = (InterceptorExpressionParser.ObjectExpressionContext) ctx.getChild(0);
            String predicate = ctx.getChild(1).getText();
            InterceptorExpressionParser.ConstExpressionContext right = (InterceptorExpressionParser.ConstExpressionContext) ctx.getChild(2);

            IArgumentMatcher matcher = null;
            switch (predicate) {
                case "=":
                    matcher = new IArgumentMatcher.EQ();
                    break;
                default:
                    throw new RuntimeException("Not supported predicate " + predicate);
            }

            // Process the left expression
            ParseTree nameExpression = left.getChild(0);
            if (nameExpression instanceof InterceptorExpressionParser.SimpleNameExpressionContext) {
                if (!"args".equals(nameExpression.getText())) {
                    throw new RuntimeException("Unsupported object name " + nameExpression.getText());
                }
                String property = left.propertyAccessorExpression().getText();
                if ("length".equals(property)) {
                    matcher.setPropertyAccessor(parameterList -> parameterList.size());
                } else {
                    throw new RuntimeException("Not supported property " + property);
                }
            } else {
                // Must be arrayAccessorExpression according to Grammar
                InterceptorExpressionParser.ArrayAccessorExpressionContext arrayAccessExpression = (InterceptorExpressionParser.ArrayAccessorExpressionContext) nameExpression;
                if (!"args".equals(arrayAccessExpression.simpleNameExpression().getText())) {
                    throw new RuntimeException("Unsupported object name " + arrayAccessExpression.simpleNameExpression().getText());
                }
                int arrayIndex = Integer.parseInt(arrayAccessExpression.UNSIGNED_INTEGER_LITERAL().getText());
                matcher.setPropertyAccessor((parameterList) -> {
                    if (parameterList.size() > arrayIndex) {
                        return parameterList.get(arrayIndex).getName();
                    }
                    return null;
                });
            }

            matcher.setExpected(right.getText());

            return matcher;
        }
    }
}
