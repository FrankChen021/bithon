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

package org.bithon.server.storage.datasource.input.filter;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.annotations.VisibleForTesting;
import lombok.Getter;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.bithon.server.datasource.input.filter.FilterExpressionBaseVisitor;
import org.bithon.server.datasource.input.filter.FilterExpressionLexer;
import org.bithon.server.datasource.input.filter.FilterExpressionParser;
import org.bithon.server.storage.datasource.aggregator.spec.InvalidExpressionException;
import org.bithon.server.storage.datasource.input.IInputRow;

import java.util.Arrays;
import java.util.Locale;
import java.util.function.Function;

/**
 * @author frank.chen021@outlook.com
 * @date 4/8/22 4:38 PM
 */
public class ExpressionFilter implements IInputRowFilter {

    @Getter
    private final String expression;

    @Getter
    private final boolean debug;

    private final IInputRowFilter delegation;

    @VisibleForTesting
    public ExpressionFilter(String expression) {
        this(expression, false);
    }

    @JsonCreator
    public ExpressionFilter(@JsonProperty("expression") String expression,
                            @JsonProperty("isDebug") Boolean debug) {
        this.expression = expression;
        this.debug = debug != null && debug;

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
        delegation = parser.prog().expression().accept(new Builder(this.debug));
    }

    @Override
    public boolean shouldInclude(IInputRow inputRow) {
        return delegation.shouldInclude(inputRow);
    }

    private static class Builder extends FilterExpressionBaseVisitor<IInputRowFilter> {
        private final boolean isDebug;

        private Builder(boolean isDebug) {
            this.isDebug = isDebug;
        }

        @Override
        public IInputRowFilter visitExpression(FilterExpressionParser.ExpressionContext ctx) {
            if (ctx.getChildCount() == 3) {

                // case 1: '(' expression ')'
                if (ctx.getChild(0) instanceof TerminalNode) {
                    return ctx.getChild(1).accept(this);
                }

                // case 2: expression logicOperator expression
                String logicOperator = ctx.getChild(1).getText().toUpperCase(Locale.ENGLISH);
                switch (logicOperator) {
                    case "OR":
                        return new OrFilter(
                            Arrays.asList(ctx.getChild(0).accept(this),
                                          ctx.getChild(2).accept(this)));
                    case "AND":
                        return new AndFilter(
                            Arrays.asList(ctx.getChild(0).accept(this),
                                          ctx.getChild(2).accept(this)));
                    default:
                        throw new RuntimeException("unknown operator: " + logicOperator);
                }
            }

            // expression or binaryExpression
            return ctx.getChild(0).accept(this);
        }

        @Override
        public IInputRowFilter visitBinaryExpression(FilterExpressionParser.BinaryExpressionContext ctx) {
            TerminalNode left = ctx.unaryExpression(0).getChild(TerminalNode.class, 0);
            String comparisonOperator = ctx.comparisonOperator().getText();
            TerminalNode right = ctx.unaryExpression(1).getChild(TerminalNode.class, 0);

            if (left.getSymbol().getType() != FilterExpressionLexer.VARIABLE) {
                // just for simplicity
                throw new InvalidExpressionException(ctx.getText(), left.getSymbol().getStartIndex(), "left expression must be a variable");
            }

            Function<IInputRow, Object> getRight;
            switch (right.getSymbol().getType()) {
                case FilterExpressionLexer.NUMBER_LITERAL: {
                    final long rightVal = Long.parseLong(right.getText());
                    getRight = inputRow -> rightVal;
                    break;
                }
                case FilterExpressionLexer.STRING_LITERAL: {
                    final String rightVal = getUnQuotedString(right.getSymbol());
                    getRight = inputRow -> rightVal;
                    break;
                }
                case FilterExpressionLexer.VARIABLE: {
                    final String rightVal = right.getText();
                    getRight = inputRow -> inputRow.getCol(rightVal);
                    break;
                }
                default:
                    throw new RuntimeException("unexpected right expression type");
            }

            BinaryExpressionFilter filter;
            String varName = left.getText();
            switch (comparisonOperator) {
                case "=":
                    filter = new BinaryExpressionFilter.EQ(inputRow -> inputRow.getCol(varName), getRight);
                    break;
                case ">":
                    filter = new BinaryExpressionFilter.GT(inputRow -> inputRow.getCol(varName), getRight);
                    break;
                case "<>":
                case "!=":
                    filter = new BinaryExpressionFilter.NE(inputRow -> inputRow.getCol(varName), getRight);
                    break;
                default:
                    throw new RuntimeException("not yet supported operator: " + comparisonOperator);
            }

            return isDebug ? new BinaryExpressionFilter.DebuggableFilter(ctx.getText(), filter) : filter;
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
