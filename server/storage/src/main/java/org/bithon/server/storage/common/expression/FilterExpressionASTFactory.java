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
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.IdentifierExpression;
import org.bithon.component.commons.expression.LiteralExpression;
import org.bithon.component.commons.expression.LogicalExpression;
import org.bithon.server.datasource.ast.FilterExpressionBaseVisitor;
import org.bithon.server.datasource.ast.FilterExpressionLexer;
import org.bithon.server.datasource.ast.FilterExpressionParser;
import org.bithon.server.storage.datasource.spec.InvalidExpressionException;

import java.util.Arrays;
import java.util.Locale;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/4/24 00:15
 */
public class FilterExpressionASTFactory {
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
                return LogicalExpression.create(logicOperator, Arrays.asList(left, right));
            }

            // expression or binaryExpression
            return ctx.getChild(0).accept(this);
        }

        @Override
        public IExpression visitBinaryExpression(FilterExpressionParser.BinaryExpressionContext ctx) {
            TerminalNode left = ctx.unaryExpression(0).getChild(TerminalNode.class, 0);
            String comparisonOperator = ctx.comparisonOperator().getText();
            TerminalNode right = ctx.unaryExpression(1).getChild(TerminalNode.class, 0);

            if (left.getSymbol().getType() != FilterExpressionLexer.VARIABLE) {
                // just for simplicity
                throw new InvalidExpressionException(ctx.getText(), left.getSymbol().getStartIndex(), "left expression must be a variable");
            }

            IExpression rightExpression;
            switch (right.getSymbol().getType()) {
                case FilterExpressionLexer.NUMBER_LITERAL: {
                    rightExpression = new LiteralExpression(Long.parseLong(right.getText()));
                    break;
                }
                case FilterExpressionLexer.STRING_LITERAL: {
                    rightExpression = new LiteralExpression(getUnQuotedString(right.getSymbol()));
                    break;
                }
                case FilterExpressionLexer.VARIABLE: {
                    rightExpression = new IdentifierExpression(right.getText());
                    break;
                }
                default:
                    throw new RuntimeException("unexpected right expression type");
            }

            BinaryExpression binaryExpression;
            String varName = left.getText();
            switch (comparisonOperator) {
                case "=":
                    binaryExpression = new BinaryExpression.EQ(new IdentifierExpression(varName), rightExpression);
                    break;
                case ">":
                    binaryExpression = new BinaryExpression.GT(new IdentifierExpression(varName), rightExpression);
                    break;
                case "<>":
                case "!=":
                    binaryExpression = new BinaryExpression.NE(new IdentifierExpression(varName), rightExpression);
                    break;
                default:
                    throw new RuntimeException("not yet supported operator: " + comparisonOperator);
            }

            return isDebug ? new BinaryExpression.DebuggableExpression(ctx.getText(), binaryExpression) : binaryExpression;
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
}
