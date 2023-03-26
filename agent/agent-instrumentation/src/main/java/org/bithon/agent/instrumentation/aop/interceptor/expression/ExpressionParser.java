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

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.bithon.agent.instrumentation.aop.interceptor.InterceptorExpressionBaseVisitor;
import org.bithon.agent.instrumentation.aop.interceptor.InterceptorExpressionLexer;
import org.bithon.agent.instrumentation.aop.interceptor.InterceptorExpressionParser;
import org.bithon.shaded.net.bytebuddy.description.method.MethodDescription;
import org.bithon.shaded.net.bytebuddy.description.method.ParameterList;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/3/26 19:32
 */
public class ExpressionParser {

    public static InterceptorExpressionParser create(String expression) {
        ErrorListener errorListener = new ErrorListener(expression);
        InterceptorExpressionLexer lexer = new InterceptorExpressionLexer(CharStreams.fromString(expression));
        lexer.getErrorListeners().clear();
        lexer.addErrorListener(errorListener);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        InterceptorExpressionParser parser = new InterceptorExpressionParser(tokens);
        parser.getErrorListeners().clear();
        parser.addErrorListener(errorListener);
        return parser;
    }

//    public static InterceptorDescriptor parse(String expression) {
//        InterceptorExpressionParser parser = create(expression);
//        parser.parse().accept(new Builder());
//        return null;
//    }

    static class ClassFilterBuilder extends InterceptorExpressionBaseVisitor<IValueSupplier> {

    }

    static class MethodFilterBuilder extends InterceptorExpressionBaseVisitor<IValueSupplier> {

        @Override
        public IValueSupplier visitSimpleNameExpression(InterceptorExpressionParser.SimpleNameExpressionContext ctx) {
            String name = ctx.getText();
            if ("method".equals(name)) {
                return MethodDescription::getName;
            }
            throw new RuntimeException("Unsupported identifier " + name);
        }

//        @Override
//        public IValueSupplier visitPropertyAccessExpression(InterceptorExpressionParser.PropertyAccessExpressionContext ctx) {
//            String objName = ctx.getChild(0).getText();
//            String propName = ctx.getChild(2).getText();
//            if ("method".equals(objName)) {
//                switch (propName) {
//                    case "isPublic":
//                        return MethodDescription::isPublic;
//                    case "isPrivate":
//                        return MethodDescription::isPrivate;
//                    case "isProtected":
//                        return MethodDescription::isProtected;
//                    case "name":
//                        return MethodDescription::getName;
//                    case "return":
//                        return methodDescription -> methodDescription.getReturnType().getTypeName();
//                    default:
//                        throw new RuntimeException("Unsupported property " + propName);
//                }
//            }
//            throw new RuntimeException("Unsupported object " + objName);
//        }

        @Override
        public IValueSupplier visitArrayAccessorExpression(InterceptorExpressionParser.ArrayAccessorExpressionContext ctx) {
            String arrayObjectName = ctx.simpleNameExpression().getText();
            int index = Integer.parseInt(ctx.UNSIGNED_INTEGER_LITERAL().getText());
            if (!"args".equals(arrayObjectName)) {
                throw new RuntimeException("Array accessor is not allowed on " + arrayObjectName);
            }
            return methodDescription -> {
                ParameterList<?> parameterList = methodDescription.getParameters();
                if (parameterList.size() > index) {
                    return parameterList.get(index);
                }
                return null;
            };
        }
    }

}
