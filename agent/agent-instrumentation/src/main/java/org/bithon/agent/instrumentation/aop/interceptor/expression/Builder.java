///*
// *    Copyright 2020 bithon.org
// *
// *    Licensed under the Apache License, Version 2.0 (the "License");
// *    you may not use this file except in compliance with the License.
// *    You may obtain a copy of the License at
// *
// *        http://www.apache.org/licenses/LICENSE-2.0
// *
// *    Unless required by applicable law or agreed to in writing, software
// *    distributed under the License is distributed on an "AS IS" BASIS,
// *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// *    See the License for the specific language governing permissions and
// *    limitations under the License.
// */
//
//package org.bithon.agent.instrumentation.aop.interceptor.expression;
//
//import org.antlr.v4.runtime.CharStream;
//import org.antlr.v4.runtime.Token;
//import org.antlr.v4.runtime.misc.Interval;
//import org.antlr.v4.runtime.tree.ParseTree;
//import org.antlr.v4.runtime.tree.TerminalNode;
//import org.bithon.agent.instrumentation.aop.interceptor.InterceptorExpressionBaseVisitor;
//import org.bithon.agent.instrumentation.aop.interceptor.InterceptorExpressionLexer;
//import org.bithon.agent.instrumentation.aop.interceptor.InterceptorExpressionParser;
//import org.bithon.agent.instrumentation.aop.interceptor.expression.comparator.Comparators;
//import org.bithon.agent.instrumentation.aop.interceptor.expression.matcher.And;
//import org.bithon.agent.instrumentation.aop.interceptor.expression.matcher.Or;
//
//import java.math.BigDecimal;
//import java.util.Arrays;
//import java.util.Locale;
//
///**
// * @author frank.chen021@outlook.com
// * @date 2023/3/26 22:12
// */
//class Builder extends InterceptorExpressionBaseVisitor<Void> {
//    private String clazzFilter;
//
//    Builder(boolean isDebug) {
//        this.isDebug = isDebug;
//    }
//
//    /**
//     * {@ink InterceptorExpression.g4}
//     *
//     * <pre>
//     * filterExpression
//     *    : binaryExpression
//     *    | unaryExpression
//     *    | filterExpression logicOperator filterExpression
//     *    | '(' filterExpression ')'
//     *    ;
//     * </pre>
//     * <p>
//     * Supported expression:
//     * method = ''
//     */
//    @Override
//    public Void visitFilterExpression(InterceptorExpressionParser.FilterExpressionContext ctx) {
//        if (ctx.getChildCount() == 3) {
//
//            // Case 1: '(' expression ')'
//            if (ctx.getChild(0) instanceof TerminalNode) {
//                return ctx.getChild(1).accept(this);
//            }
//
//            // Case 2: expression logicOperator expression
//            String logicOperator = ctx.getChild(1).getText().toUpperCase(Locale.ENGLISH);
//            switch (logicOperator) {
//                case "OR":
//                    return new Or(
//                        Arrays.asList(ctx.getChild(0).accept(this),
//                                      ctx.getChild(2).accept(this)));
//                case "AND":
//                    return new And(
//                        Arrays.asList(ctx.getChild(0).accept(this),
//                                      ctx.getChild(2).accept(this)));
//                default:
//                    throw new RuntimeException("unknown operator: " + logicOperator);
//            }
//        }
//
//        // Either a binaryExpression or unaryExpression
//        return ctx.getChild(0).accept(this);
//    }
//
//    /**
//     * class.name = '' and class.is
//     *
//     * <pre>
//     *  binaryExpression
//     *    : unaryExpression comparisonOperator unaryExpression
//     *    |
//     *    ;
//     *
//     * unaryExpression
//     *    : constExpression
//     *    | objectExpression
//     *    | functionExpression
//     *    ;
//     *
//     * objectExpression
//     *    : simpleNameExpression
//     *    | arrayAccessorExpression
//     *    | propertyAccessExpression
//     *    ;
//     *
//     * arrayAccessorExpression
//     *    : simpleNameExpression '[' UNSIGNED_INTEGER_LITERAL ']'
//     *    ;
//     *
//     * </pre>
//     */
//    @Override
//    public Void visitBinaryExpression(InterceptorExpressionParser.BinaryExpressionContext ctx) {
//        ParseTree left = ctx.unaryExpression(0).getChild(0);
//        String comparisonOperator = ctx.comparisonOperator().getText();
//        ParseTree right = ctx.unaryExpression(1).getChild(0);
//
//        if (left instanceof InterceptorExpressionParser.ConstExpressionContext) {
//            // Left expression can't be const expression,
//            // This restriction helps simplifies the processing
//            throw new RuntimeException(String.format(Locale.ENGLISH,
//                                                     "%s %d %d %s",
//                                                     ctx.getText(),
//                                                     ((InterceptorExpressionParser.ConstExpressionContext) left).start.getLine(),
//                                                     ((InterceptorExpressionParser.ConstExpressionContext) left).start.getCharPositionInLine(),
//                                                     "left expression must be a variable"));
//        }
//
//        IValueSupplier rightValueSupplier = null;
//        if (right instanceof InterceptorExpressionParser.ConstExpressionContext) {
//            TerminalNode constExpression = (TerminalNode) ((InterceptorExpressionParser.ConstExpressionContext) right).children.get(0);
//
//            switch (constExpression.getSymbol().getType()) {
//                case InterceptorExpressionLexer.UNSIGNED_INTEGER_LITERAL: {
//                    final long rightVal = Long.parseLong(constExpression.getText());
//                    rightValueSupplier = value -> rightVal;
//                    break;
//                }
//                case InterceptorExpressionLexer.DECIMAL_LITERAL: {
//                    final BigDecimal rightVal = new BigDecimal(constExpression.getText());
//                    rightValueSupplier = value -> rightVal;
//                    break;
//                }
//                case InterceptorExpressionLexer.STRING_LITERAL: {
//                    final String rightVal = getUnQuotedString(constExpression.getSymbol());
//                    rightValueSupplier = value -> rightVal;
//                    break;
//                }
//                default:
//                    break;
//            }
//        }
//        if (rightValueSupplier == null) {
//            throw new RuntimeException("Unexpected right expression type: " + right.getClass().getName());
//        }
//
//        if (left instanceof InterceptorExpressionParser.ObjectExpressionContext) {
//            String objName = left.accept(new InterceptorExpressionBaseVisitor<String>() {
//                @Override
//                public String visitSimpleNameExpression(InterceptorExpressionParser.SimpleNameExpressionContext ctx) {
//                    return ctx.getText();
//                }
//            });
//            if ("class".equals(objName)) {
//
//            }
//
//            switch (comparisonOperator) {
//                case "=":
//                    new Comparators.EQ(left.accept(new ExpressionParser.MethodFilterBuilder()), rightValueSupplier);
//                    break;
//                default:
//                    throw new RuntimeException("not yet supported operator: " + comparisonOperator);
//            }
//        }
//        throw new RuntimeException("Unsupported expression " + left.getClass().getSimpleName());
//    }
//
//    private String getUnQuotedString(Token symbol) {
//        CharStream input = symbol.getInputStream();
//        if (input == null) {
//            return null;
//        } else {
//            int n = input.size();
//            int s = symbol.getStartIndex() + 1; // +1 to skip the leading quoted character
//            int e = symbol.getStopIndex() - 1;  // -1 to skip the ending quoted character
//            return s < n && e < n ? input.getText(Interval.of(s, e)) : "<EOF>";
//        }
//    }
//}
