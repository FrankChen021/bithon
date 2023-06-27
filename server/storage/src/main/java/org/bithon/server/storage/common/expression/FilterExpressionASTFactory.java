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

package org.bithon.server.storage.common.expression;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.bithon.component.commons.expression.BinaryExpression;
import org.bithon.component.commons.expression.ExpressionList;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.IdentifierExpression;
import org.bithon.component.commons.expression.LiteralExpression;
import org.bithon.component.commons.expression.LogicalExpression;
import org.bithon.component.commons.expression.debug.DebuggableExpression;
import org.bithon.component.commons.expression.debug.DebuggableLogicExpression;
import org.bithon.server.datasource.ast.FilterExpressionBaseVisitor;
import org.bithon.server.datasource.ast.FilterExpressionLexer;
import org.bithon.server.datasource.ast.FilterExpressionParser;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/4/24 00:15
 */
public class FilterExpressionASTFactory {

    public static IExpression create(String expression) {
        return create(expression, false);
    }

    public static IExpression create(String expression, boolean debug) {
        FilterExpressionLexer lexer = new FilterExpressionLexer(CharStreams.fromString(expression));
        lexer.getErrorListeners().clear();
        lexer.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer,
                                    Object offendingSymbol,
                                    int line,
                                    int charPositionInLine,
                                    String msg,
                                    RecognitionException e) {
                throw new InvalidExpressionException(expression, charPositionInLine, msg);
            }
        });
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        FilterExpressionParser parser = new FilterExpressionParser(tokens);
        parser.getErrorListeners().clear();
        parser.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer,
                                    Object offendingSymbol,
                                    int line,
                                    int charPositionInLine,
                                    String msg,
                                    RecognitionException e) {
                throw new InvalidExpressionException(expression, charPositionInLine, msg);
            }
        });
        return parser.parse().filterExpression().accept(new Builder(debug));
    }

    private static class Builder extends FilterExpressionBaseVisitor<IExpression> {
        private final boolean isDebug;

        private Builder(boolean isDebug) {
            this.isDebug = isDebug;
        }

        @Override
        public IExpression visitFilterExpression(FilterExpressionParser.FilterExpressionContext ctx) {
            if (ctx.getChildCount() == 3) {

                //
                // Case 1: '(' expression ')'
                //
                if (ctx.getChild(0) instanceof TerminalNode) {
                    return ctx.getChild(1).accept(this);
                }

                //
                // Case 2: expression logicOperator expression
                //
                IExpression left = ctx.getChild(0).accept(this);
                String logicOperator = ctx.getChild(1).getText().toUpperCase(Locale.ENGLISH);
                IExpression right = ctx.getChild(2).accept(this);
                LogicalExpression logicalExpression = LogicalExpression.create(logicOperator, Arrays.asList(left, right));
                if (this.isDebug) {
                    return new DebuggableLogicExpression(logicalExpression);
                } else {
                    return logicalExpression;
                }
            }

            // expression or binaryExpression
            return ctx.getChild(0).accept(this);
        }

        @Override
        public IExpression visitBinaryExpression(FilterExpressionParser.BinaryExpressionContext ctx) {
            FilterExpressionParser.UnaryExpressionContext left = ctx.unaryExpression(0);
            String comparisonOperator = ctx.getChild(1).getText().toLowerCase(Locale.ENGLISH);

            if (left.nameExpression() == null) {
                // just for simplicity
                throw new InvalidExpressionException(ctx.getText(), left.start.getStartIndex(), "left expression must be a name");
            }

            IExpression leftExpression = new IdentifierExpression(left.getText());
            BinaryExpression binaryExpression;
            switch (comparisonOperator) {
                case "=":
                    binaryExpression = new BinaryExpression.EQ(leftExpression, ctx.unaryExpression(1).accept(new UnaryExpressionVisitor()));
                    break;
                case ">":
                    binaryExpression = new BinaryExpression.GT(leftExpression, ctx.unaryExpression(1).accept(new UnaryExpressionVisitor()));
                    break;
                case "<>":
                case "!=":
                    binaryExpression = new BinaryExpression.NE(leftExpression, ctx.unaryExpression(1).accept(new UnaryExpressionVisitor()));
                    break;
                case "in":
                    binaryExpression = new BinaryExpression.IN(leftExpression, ctx.experssionList().accept(this));
                    break;
                case "like":
                    binaryExpression = new BinaryExpression.LIKE(leftExpression, ctx.unaryExpression(1).accept(new UnaryExpressionVisitor()));
                    break;
                default:
                    throw new RuntimeException("not yet supported operator: " + comparisonOperator);
            }

            if (isDebug) {
                return new DebuggableExpression(binaryExpression);
            } else {
                return binaryExpression;
            }
        }

        @Override
        public IExpression visitExperssionList(FilterExpressionParser.ExperssionListContext ctx) {
            final UnaryExpressionVisitor unaryExpressionGenerator = new UnaryExpressionVisitor();
            return new ExpressionList(ctx.unaryExpression()
                                         .stream()
                                         .map((expr) -> expr.accept(unaryExpressionGenerator))
                                         .collect(Collectors.toList()));
        }
    }

    static String getUnQuotedString(Token symbol) {
        CharStream input = symbol.getInputStream();
        if (input == null) {
            return null;
        } else {
            int n = input.size();

            // +1 to skip the leading quoted character
            int s = symbol.getStartIndex() + 1;

            // -1 to skip the ending quoted character
            int e = symbol.getStopIndex() - 1;
            return s < n && e < n ? input.getText(Interval.of(s, e)) : "<EOF>";
        }
    }

    /**
     * Turn into UnaryExpression into customer AST
     */
    static class UnaryExpressionVisitor extends FilterExpressionBaseVisitor<IExpression> {
        @Override
        public IExpression visitLiteralExperssion(FilterExpressionParser.LiteralExperssionContext ctx) {
            TerminalNode literalExpressionNode = ctx.getChild(TerminalNode.class, 0);
            switch (literalExpressionNode.getSymbol().getType()) {
                case FilterExpressionLexer.NUMBER_LITERAL: {
                    return new LiteralExpression(Long.parseLong(literalExpressionNode.getText()));
                }
                case FilterExpressionLexer.STRING_LITERAL: {
                    return new LiteralExpression(getUnQuotedString(literalExpressionNode.getSymbol()));
                }
                default:
                    throw new RuntimeException("unexpected right expression type");
            }
        }

        @Override
        public IExpression visitNameExpression(FilterExpressionParser.NameExpressionContext ctx) {
            return new IdentifierExpression(ctx.getText());
        }
    }

}
