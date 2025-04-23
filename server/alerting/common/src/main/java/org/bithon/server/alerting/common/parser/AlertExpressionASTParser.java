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

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.LiteralExpression;
import org.bithon.component.commons.expression.LogicalExpression;
import org.bithon.component.commons.expression.expt.InvalidExpressionException;
import org.bithon.component.commons.utils.CollectionUtils;
import org.bithon.component.commons.utils.HumanReadableDuration;
import org.bithon.component.commons.utils.HumanReadablePercentage;
import org.bithon.server.alerting.common.evaluator.metric.IMetricEvaluator;
import org.bithon.server.alerting.common.evaluator.metric.absolute.EqualPredicate;
import org.bithon.server.alerting.common.evaluator.metric.absolute.GreaterOrEqualPredicate;
import org.bithon.server.alerting.common.evaluator.metric.absolute.GreaterThanPredicate;
import org.bithon.server.alerting.common.evaluator.metric.absolute.LessThanOrEqualPredicate;
import org.bithon.server.alerting.common.evaluator.metric.absolute.LessThanPredicate;
import org.bithon.server.alerting.common.evaluator.metric.absolute.NotEqualPredicate;
import org.bithon.server.alerting.common.evaluator.metric.absolute.NullValuePredicate;
import org.bithon.server.alerting.common.evaluator.metric.relative.RelativeGTEPredicate;
import org.bithon.server.alerting.common.evaluator.metric.relative.RelativeGTPredicate;
import org.bithon.server.alerting.common.evaluator.metric.relative.RelativeLTEPredicate;
import org.bithon.server.alerting.common.evaluator.metric.relative.RelativeLTPredicate;
import org.bithon.server.alerting.common.model.AlertExpression;
import org.bithon.server.commons.antlr4.SyntaxErrorListener;
import org.bithon.server.metric.expression.MetricExpressionBaseVisitor;
import org.bithon.server.metric.expression.MetricExpressionLexer;
import org.bithon.server.metric.expression.MetricExpressionParser;
import org.bithon.server.metric.expression.ast.MetricExpression;
import org.bithon.server.metric.expression.ast.MetricExpressionASTBuilder;

import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2024/1/7 18:18
 */
public class AlertExpressionASTParser {

    /**
     * The return is either a MetricExpression or a LogicalExpression
     */
    public static IExpression parse(String expression) {
        MetricExpressionLexer lexer = new MetricExpressionLexer(CharStreams.fromString(expression));
        lexer.getErrorListeners().clear();
        lexer.addErrorListener(SyntaxErrorListener.of(expression));
        CommonTokenStream tokens = new CommonTokenStream(lexer);

        MetricExpressionParser parser = new MetricExpressionParser(tokens);
        parser.getErrorListeners().clear();
        parser.addErrorListener(SyntaxErrorListener.of(expression));

        MetricExpressionParser.AlertExpressionContext ctx = parser.alertExpression();
        Token last = tokens.LT(1);
        if (last.getType() != MetricExpressionParser.EOF) {
            throw InvalidExpressionException.format(expression,
                                                    last.getStartIndex(),
                                                    last.getStopIndex(),
                                                    last.getLine(),
                                                    last.getStartIndex(),
                                                    "Unexpected token: " + last.getText());
        }

        return ctx.accept(new MetricExpressionBuilder());
    }

    private static class MetricExpressionBuilder extends MetricExpressionBaseVisitor<IExpression> {
        private int index = 1;

        @Override
        public IExpression visitAtomicAlertExpression(MetricExpressionParser.AtomicAlertExpressionContext ctx) {
            IExpression expression = MetricExpressionASTBuilder.build(ctx.metricExpression());
            if (!(expression instanceof MetricExpression metricExpression)) {
                throw new InvalidExpressionException("Given expression [%s] is not an expression with predicate.", ctx.getText());
            }

            if (metricExpression.getPredicate() == null) {
                // the general metric expression allows null predicate, but for alerting, it's a compulsory field
                throw new InvalidExpressionException("Missing predicate expression");
            }
            if (metricExpression.getWindow() == null) {
                // the alert expression now requires a 1-minute window
                metricExpression.setWindow(HumanReadableDuration.DURATION_1_MINUTE);
            }

            AlertExpression alertExpression = new AlertExpression();
            alertExpression.setMetricExpression(metricExpression);
            alertExpression.setId(String.valueOf(index++));
            alertExpression.setMetricEvaluator(createMetricEvaluator(metricExpression));
            return alertExpression;
        }

        @Override
        public IExpression visitParenthesisAlertExpression(MetricExpressionParser.ParenthesisAlertExpressionContext ctx) {
            return ctx.alertExpression().accept(this);
        }

        @Override
        public IExpression visitLogicalAlertExpression(MetricExpressionParser.LogicalAlertExpressionContext ctx) {
            List<MetricExpressionParser.AlertExpressionContext> contextList = ctx.alertExpression();

            IExpression left = contextList.get(0).accept(this);
            IExpression right = contextList.get(1).accept(this);

            if (left instanceof AlertExpression leftAlertExpression
                && right instanceof AlertExpression rightAlertExpression) {

                // For two non-empty GROUP-BY, the two sets MUST be the same
                if (CollectionUtils.isNotEmpty(leftAlertExpression.getMetricExpression().getGroupBy())
                    && CollectionUtils.isNotEmpty(rightAlertExpression.getMetricExpression().getGroupBy())) {

                    if (!leftAlertExpression.getMetricExpression()
                                            .getGroupBy()
                                            .equals(rightAlertExpression.getMetricExpression().getGroupBy())) {

                        throw new InvalidExpressionException("The BY expression of the two expression [%s] , [%s] are NOT the same.",
                                                             left.serializeToText(),
                                                             right.serializeToText());
                    }
                }
            }

            TerminalNode op = ctx.getChild(TerminalNode.class, 0);
            if (op.getSymbol().getType() == MetricExpressionParser.AND) {
                return new LogicalExpression.AND(left, right);
            } else {
                return new LogicalExpression.OR(left, right);
            }
        }
    }

    private static IMetricEvaluator createMetricEvaluator(MetricExpression metricExpression) {
        LiteralExpression<?> expected = metricExpression.getExpected();
        HumanReadableDuration offset = metricExpression.getOffset();

        IMetricEvaluator metricEvaluator;
        switch (metricExpression.getPredicate()) {
            case LT:
                if (offset == null) {
                    metricEvaluator = new LessThanPredicate(expected.getValue());
                } else {
                    if (expected.getValue() instanceof HumanReadablePercentage) {
                        metricEvaluator = new RelativeLTPredicate((Number) expected.getValue(), offset);
                    } else {
                        metricEvaluator = new LessThanPredicate(expected.getValue());
                    }
                }
                break;

            case LTE:
                if (offset == null) {
                    metricEvaluator = new LessThanOrEqualPredicate(expected.getValue());
                } else {
                    if (expected.getValue() instanceof HumanReadablePercentage) {
                        metricEvaluator = new RelativeLTEPredicate((Number) expected.getValue(), offset);
                    } else {
                        metricEvaluator = new LessThanOrEqualPredicate(expected.getValue());
                    }
                }
                break;

            case GT:
                if (offset == null) {
                    metricEvaluator = new GreaterThanPredicate(expected.getValue());
                } else {
                    if (expected.getValue() instanceof HumanReadablePercentage) {
                        metricEvaluator = new RelativeGTPredicate((Number) expected.getValue(), offset);
                    } else {
                        metricEvaluator = new GreaterThanPredicate(expected.getValue());
                    }
                }
                break;

            case GTE:
                if (offset == null) {
                    metricEvaluator = new GreaterOrEqualPredicate(expected.getValue());
                } else {
                    if (expected.getValue() instanceof HumanReadablePercentage) {
                        metricEvaluator = new RelativeGTEPredicate((Number) expected.getValue(), offset);
                    } else {
                        metricEvaluator = new GreaterThanPredicate(expected.getValue());
                    }
                }
                break;

            case NE:
                metricEvaluator = new NotEqualPredicate(expected.getValue());
                break;

            case EQ:
                metricEvaluator = new EqualPredicate(expected.getValue());
                break;

            case IS_NULL:
                metricEvaluator = new NullValuePredicate();
                break;

            default:
                throw new UnsupportedOperationException("Unsupported predicate");
        }

        return metricEvaluator;
    }
}
