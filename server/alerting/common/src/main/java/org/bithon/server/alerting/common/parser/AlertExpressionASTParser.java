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

package org.bithon.server.alerting.common.parser;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.LogicalExpression;
import org.bithon.server.alerting.common.model.AlertExpression;
import org.bithon.server.alerting.common.model.MetricExpression;

import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2024/1/7 18:18
 */
public class AlertExpressionASTParser {

    public static IExpression parse(String expression) {
        MetricExpressionLexer lexer = new MetricExpressionLexer(CharStreams.fromString(expression));
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
        MetricExpressionParser parser = new MetricExpressionParser(tokens);
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

        MetricExpressionParser.AlertExpressionContext ctx = parser.alertExpression();
        if (tokens.LT(1).getType() != MetricExpressionParser.EOF) {
            throw new InvalidExpressionException(expression, tokens.LT(1).getStartIndex(), "Unexpected token");
        }

        return ctx.accept(new MetricExpressionBuilder());
    }

    private static class MetricExpressionBuilder extends MetricExpressionBaseVisitor<IExpression> {
        private int index = 1;

        @Override
        public IExpression visitSimpleAlertExpression(MetricExpressionParser.SimpleAlertExpressionContext ctx) {
            MetricExpression expression = MetricExpressionASTBuilder.build(ctx.metricExpression());
            AlertExpression alertExpression = new AlertExpression();
            alertExpression.setMetricExpression(expression);
            alertExpression.setId(String.valueOf(index++));
            return alertExpression;
        }

        @Override
        public IExpression visitParenthesisAlertExpression(MetricExpressionParser.ParenthesisAlertExpressionContext ctx) {
            return ctx.alertExpression().accept(this);
        }

        @Override
        public IExpression visitLogicalAlertExpression(MetricExpressionParser.LogicalAlertExpressionContext ctx) {
            List<MetricExpressionParser.AlertExpressionContext> contextList = ctx.alertExpression();
            MetricExpressionParser.AlertExpressionContext left = contextList.get(0);
            MetricExpressionParser.AlertExpressionContext right = contextList.get(1);
            TerminalNode op = ctx.getChild(TerminalNode.class, 0);
            if (op.getSymbol().getType() == MetricExpressionParser.AND) {
                return new LogicalExpression.AND(left.accept(this), right.accept(this));
            } else {
                return new LogicalExpression.OR(left.accept(this), right.accept(this));
            }
        }
    }
}
