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
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.bithon.component.commons.expression.ComparisonExpression;
import org.bithon.component.commons.expression.ConditionalExpression;
import org.bithon.component.commons.expression.ExpressionList;
import org.bithon.component.commons.expression.IDataType;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.IdentifierExpression;
import org.bithon.component.commons.expression.LiteralExpression;
import org.bithon.component.commons.expression.LogicalExpression;
import org.bithon.component.commons.utils.HumanReadableDuration;
import org.bithon.component.commons.utils.HumanReadablePercentage;
import org.bithon.component.commons.utils.HumanReadableSize;
import org.bithon.component.commons.utils.StringUtils;
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
import org.bithon.server.alerting.common.model.AggregatorEnum;
import org.bithon.server.alerting.common.model.AlertExpression;
import org.bithon.server.web.service.datasource.api.QueryField;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 2024/1/7 18:18
 */
public class AlertExpressionASTParser {

    public static IExpression parse(String expression) {
        AlertExpressionLexer lexer = new AlertExpressionLexer(CharStreams.fromString(expression));
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
        AlertExpressionParser parser = new AlertExpressionParser(tokens);
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

        return parser.parse()
                     .expression()
                     .accept(new AlertExpressionBuilder());
    }

    static void checkIfTrue(boolean expression, String message) {
        if (!expression) {
            throw new InvalidExpressionException(message);
        }
    }

    static <T> T checkNotNull(T value, String message) {
        if (value == null) {
            throw new InvalidExpressionException(message);
        }
        return value;
    }

    private static class AlertExpressionBuilder extends AlertExpressionBaseVisitor<IExpression> {
        private int index = 1;

        @Override
        public IExpression visitAlertExpression(AlertExpressionParser.AlertExpressionContext ctx) {
            AlertExpressionParser.SelectExpressionContext selectExpression = ctx.selectExpression();
            String[] names = selectExpression.metricExpression().getText().split("\\.");
            String from = names[0];
            String metric = names[1];

            String aggregatorText = selectExpression.aggregatorExpression().getText().toLowerCase(Locale.ENGLISH);
            AggregatorEnum aggregator;
            try {
                aggregator = AggregatorEnum.valueOf(aggregatorText);
            } catch (RuntimeException ignored) {
                throw new InvalidExpressionException(StringUtils.format("The aggregator [%s] in the expression is not supported", aggregatorText));
            }

            HumanReadableDuration duration = HumanReadableDuration.DURATION_1_MINUTE;
            AlertExpressionParser.DurationExpressionContext windowExpressionCtx = selectExpression.durationExpression();
            if (windowExpressionCtx != null) {
                duration = windowExpressionCtx.accept(new DurationExpressionBuilder());
                if (duration.isNegative()) {
                    throw new InvalidExpressionException(StringUtils.format("The integer literal in duration expression '%s' must be greater than zero", windowExpressionCtx.getText()));
                }
                if (duration.isZero()) {
                    throw new InvalidExpressionException(StringUtils.format("The integer literal in duration expression '%s' must be greater than zero", windowExpressionCtx.getText()));
                }
            }

            IExpression whereExpression = null;
            AlertExpressionParser.WhereExpressionContext where = selectExpression.whereExpression();
            if (where != null) {
                FilterExpressionBuilder filterASTBuilder = new FilterExpressionBuilder();
                List<IExpression> filters = where.filterExpression()
                                                 .stream()
                                                 .map((filter) -> filter.accept(filterASTBuilder))
                                                 .collect(Collectors.toList());
                if (filters.size() == 1) {
                    whereExpression = filters.get(0);
                } else if (filters.size() > 1) {
                    whereExpression = new LogicalExpression.AND(filters);
                }
            }

            List<String> groupBy = null;
            AlertExpressionParser.GroupByExpressionContext groupByExpression = selectExpression.groupByExpression();
            if (groupByExpression != null) {
                groupBy = groupByExpression.IDENTIFIER()
                                           .stream()
                                           .map((identifier) -> identifier.getSymbol().getText())
                                           .collect(Collectors.toList());
            }

            //
            // Alert Predicate
            //
            AlertExpressionParser.AlertExpectedExpressionContext expectedExpression = ctx.alertExpectedExpression();
            LiteralExpression expected = expectedExpression.literalExpression().accept(new LiteralExpressionBuilder());

            HumanReadableDuration expectedWindow = null;
            AlertExpressionParser.DurationExpressionContext expectedWindowCtx = expectedExpression.durationExpression();
            if (expectedWindowCtx != null) {
                expectedWindow = expectedWindowCtx.accept(new DurationExpressionBuilder());
                if (!expectedWindow.isNegative()) {
                    throw new InvalidExpressionException("The value in the expected window expression '%s' must be a negative value.", expectedWindowCtx.getText());
                }

                if (expectedWindow.isZero()) {
                    throw new InvalidExpressionException("The value in the expected window expression '%s' can't be zero.", expectedWindowCtx.getText());
                }
            }

            IMetricEvaluator metricEvaluator;
            AlertExpressionParser.AlertPredicateExpressionContext predicateExpression = ctx.alertPredicateExpression();
            TerminalNode predicateTerminal = predicateExpression.getChild(TerminalNode.class, 0);
            switch (predicateTerminal.getSymbol().getType()) {
                case AlertExpressionParser.LT:
                    checkNotNull(expected, "null is not allowed after predicate '<'.");
                    if (expectedWindow == null) {
                        metricEvaluator = new LessThanPredicate(expected.getValue());
                    } else {
                        if (expected.getValue() instanceof HumanReadablePercentage) {
                            metricEvaluator = new RelativeLTPredicate((Number) expected.getValue(), expectedWindow);
                        } else {
                            metricEvaluator = new LessThanPredicate(expected.getValue());
                        }
                    }
                    break;

                case AlertExpressionParser.LTE:
                    checkNotNull(expected, "null is not allowed after predicate '<='.");
                    if (expectedWindow == null) {
                        metricEvaluator = new LessThanOrEqualPredicate(expected.getValue());
                    } else {
                        if (expected.getValue() instanceof HumanReadablePercentage) {
                            metricEvaluator = new RelativeLTEPredicate((Number) expected.getValue(), expectedWindow);
                        } else {
                            metricEvaluator = new LessThanOrEqualPredicate(expected.getValue());
                        }
                    }
                    break;

                case AlertExpressionParser.GT:
                    checkNotNull(expected, "null is not allowed after predicate '>'.");
                    if (expectedWindow == null) {
                        metricEvaluator = new GreaterThanPredicate(expected.getValue());
                    } else {
                        if (expected.getValue() instanceof HumanReadablePercentage) {
                            metricEvaluator = new RelativeGTPredicate((Number) expected.getValue(), expectedWindow);
                        } else {
                            metricEvaluator = new GreaterThanPredicate(expected.getValue());
                        }
                    }
                    break;

                case AlertExpressionParser.GTE:
                    checkNotNull(expected, "null is not allowed after predicate '>='.");
                    if (expectedWindow == null) {
                        metricEvaluator = new GreaterOrEqualPredicate(expected.getValue());
                    } else {
                        if (expected.getValue() instanceof HumanReadablePercentage) {
                            metricEvaluator = new RelativeGTEPredicate((Number) expected.getValue(), expectedWindow);
                        } else {
                            metricEvaluator = new GreaterThanPredicate(expected.getValue());
                        }
                    }
                    break;

                case AlertExpressionParser.NE:
                    checkNotNull(expected, "null is not allowed after predicate '<>'.");
                    checkIfTrue(expectedWindow == null, "<> is not allowed for relative comparison.");
                    metricEvaluator = new NotEqualPredicate(expected.getValue());
                    break;

                case AlertExpressionParser.EQ:
                    checkNotNull(expected, "null is not allowed after predicate '='.");
                    checkIfTrue(expectedWindow == null, "= is not allowed for relative comparison.");
                    metricEvaluator = new EqualPredicate(expected.getValue());
                    break;

                case AlertExpressionParser.IS:
                    checkIfTrue(expected == null, "Only 'null' is allowed after the 'is' predicate.");
                    checkIfTrue(expectedWindow == null, "The expected window must be NULL if the 'is' operator is used.");
                    metricEvaluator = new NullValuePredicate();
                    break;

                default:
                    throw new RuntimeException("Unsupported alert predicate");
            }

            AlertExpression expression = new AlertExpression();
            expression.setId(String.valueOf(index++));
            expression.setFrom(from);
            expression.setWhereExpression(whereExpression);

            // For 'count' aggregator, use the 'count' as output column instead of using the column name as output name
            expression.setSelect(new QueryField(aggregator.equals(AggregatorEnum.count) ? "count" : metric, metric, aggregator.name()));
            expression.setGroupBy(groupBy);
            expression.setWindow(duration);
            expression.setAlertPredicate(predicateTerminal.getText().toLowerCase(Locale.ENGLISH));
            expression.setAlertExpected(expected == null ? null : expected.getValue());
            expression.setExpectedWindow(expectedWindow);
            expression.setMetricEvaluator(metricEvaluator);
            return expression;
        }

        @Override
        public IExpression visitBraceAlertExpression(AlertExpressionParser.BraceAlertExpressionContext ctx) {
            return ctx.expression().accept(this);
        }

        @Override
        public IExpression visitLogicalAlertExpression(AlertExpressionParser.LogicalAlertExpressionContext ctx) {
            List<AlertExpressionParser.ExpressionContext> contextList = ctx.expression();
            AlertExpressionParser.ExpressionContext left = contextList.get(0);
            AlertExpressionParser.ExpressionContext right = contextList.get(1);
            TerminalNode op = ctx.getChild(TerminalNode.class, 0);
            if (op.getSymbol().getType() == AlertExpressionParser.AND) {
                return new LogicalExpression.AND(left.accept(this), right.accept(this));
            } else {
                return new LogicalExpression.OR(left.accept(this), right.accept(this));
            }
        }
    }

    private static class DurationExpressionBuilder extends AlertExpressionBaseVisitor<HumanReadableDuration> {
        @Override
        public HumanReadableDuration visitDurationExpression(AlertExpressionParser.DurationExpressionContext ctx) {
            TerminalNode duration = (TerminalNode) ctx.getChild(1);
            return HumanReadableDuration.parse(duration.getText());
        }
    }

    private static class FilterExpressionBuilder extends AlertExpressionBaseVisitor<IExpression> {

        @Override
        public IExpression visitLiteralExpression(AlertExpressionParser.LiteralExpressionContext ctx) {
            return ctx.getChild(0).accept(new LiteralExpressionBuilder());
        }

        @Override
        public IExpression visitLiteralListExpression(AlertExpressionParser.LiteralListExpressionContext ctx) {
            LiteralExpressionBuilder builder = new LiteralExpressionBuilder();
            List<IExpression> expressionList = ctx.literalExpression()
                                                  .stream()
                                                  .map((literalExpression) -> literalExpression.accept(builder))
                                                  .collect(Collectors.toList());

            IDataType literalType = expressionList.get(0).getDataType();
            for (int i = 1; i < expressionList.size(); i++) {
                if (!literalType.equals(expressionList.get(i).getDataType())) {
                    throw new InvalidExpressionException("The type in the expression list[%s] must be the same.", ctx.getText());
                }
            }
            return new ExpressionList(expressionList);
        }

        @Override
        public IExpression visitComparisonExpression(AlertExpressionParser.ComparisonExpressionContext ctx) {
            IdentifierExpression identifier = new IdentifierExpression(ctx.IDENTIFIER().getSymbol().getText());
            IExpression expected = ctx.literalExpression().accept(this);

            TerminalNode operator = ctx.getChild(TerminalNode.class, 1);
            return switch (operator.getSymbol().getType()) {
                case AlertExpressionParser.LT -> {
                    checkIfTrue(expected instanceof LiteralExpression, "The expected value of '<' operator must be type of literal.");
                    yield new ComparisonExpression.LT(identifier, expected);
                }
                case AlertExpressionParser.LTE -> {
                    checkIfTrue(expected instanceof LiteralExpression, "The expected value of '<=' operator must be type of literal.");
                    yield new ComparisonExpression.LTE(identifier, expected);
                }
                case AlertExpressionParser.GT -> {
                    checkIfTrue(expected instanceof LiteralExpression, "The expected value of '>' operator must be type of literal.");
                    yield new ComparisonExpression.GT(identifier, expected);
                }
                case AlertExpressionParser.GTE -> {
                    checkIfTrue(expected instanceof LiteralExpression, "The expected value of '>=' operator must be type of literal.");
                    yield new ComparisonExpression.GTE(identifier, expected);
                }
                case AlertExpressionParser.NE -> {
                    checkIfTrue(expected instanceof LiteralExpression, "The expected value of '<>' operator must be type of literal.");
                    yield new ComparisonExpression.NE(identifier, expected);
                }
                case AlertExpressionParser.EQ -> {
                    checkIfTrue(expected instanceof LiteralExpression, "The expected value of '=' operator must be type of literal.");
                    yield new ComparisonExpression.EQ(identifier, expected);
                }
                default -> throw new RuntimeException("Unsupported operator type: " + operator.getSymbol().getText());
            };
        }

        @Override
        public IExpression visitInFilterExpression(AlertExpressionParser.InFilterExpressionContext ctx) {
            IdentifierExpression identifier = new IdentifierExpression(ctx.IDENTIFIER().getSymbol().getText());
            IExpression expected = ctx.literalListExpression().accept(this);

            checkIfTrue(expected instanceof ExpressionList, "The expected value of 'in' operator must be type of literal list.");
            return new ConditionalExpression.In(identifier, (ExpressionList) expected);
        }

        @Override
        public IExpression visitNotInFilterExpression(AlertExpressionParser.NotInFilterExpressionContext ctx) {
            IdentifierExpression identifier = new IdentifierExpression(ctx.IDENTIFIER().getSymbol().getText());
            IExpression expected = ctx.literalListExpression().accept(this);

            checkIfTrue(expected instanceof ExpressionList, "The expected value of 'not in' operator must be type of literal list.");
            return new ConditionalExpression.NotIn(identifier, (ExpressionList) expected);
        }

        @Override
        public IExpression visitNotLikeFilterExpression(AlertExpressionParser.NotLikeFilterExpressionContext ctx) {
            IdentifierExpression identifier = new IdentifierExpression(ctx.IDENTIFIER().getSymbol().getText());
            IExpression expected = ctx.literalExpression().accept(this);

            checkIfTrue(expected instanceof LiteralExpression, "The expected value of 'not like' operator must be type of literal.");
            checkIfTrue(IDataType.STRING.equals(expected.getDataType()), "The literal of 'not like' operator must be type of String.");
            return new ConditionalExpression.NotLike(identifier, expected);
        }

        @Override
        public IExpression visitLikeExpression(AlertExpressionParser.LikeExpressionContext ctx) {
            IdentifierExpression identifier = new IdentifierExpression(ctx.IDENTIFIER().getSymbol().getText());
            IExpression expected = ctx.literalExpression().accept(this);

            checkIfTrue(expected instanceof LiteralExpression, "The expected value of 'like' operator must be type of literal.");
            checkIfTrue(IDataType.STRING.equals(expected.getDataType()), "The literal of 'like' operator must be type of String.");
            return new ConditionalExpression.Like(identifier, expected);
        }

        @Override
        public IExpression visitEndwithExpression(AlertExpressionParser.EndwithExpressionContext ctx) {
            //TODO:
            return super.visitEndwithExpression(ctx);
        }

        @Override
        public IExpression visitStartwithExpression(AlertExpressionParser.StartwithExpressionContext ctx) {
            //TODO:
            return super.visitStartwithExpression(ctx);
        }

        @Override
        public IExpression visitContainsExpression(AlertExpressionParser.ContainsExpressionContext ctx) {
            //TODO:
            return super.visitContainsExpression(ctx);
        }

        @Override
        public IExpression visitHasExpression(AlertExpressionParser.HasExpressionContext ctx) {
            //TODO:
            return super.visitHasExpression(ctx);
        }
    }

    private static class LiteralExpressionBuilder extends AlertExpressionBaseVisitor<LiteralExpression> {
        @Override
        public LiteralExpression visitTerminal(TerminalNode node) {
            Token symbol = node.getSymbol();
            return switch (symbol.getType()) {
                case AlertExpressionParser.DECIMAL_LITERAL -> LiteralExpression.create(parseDecimal(symbol.getText()));
                case AlertExpressionParser.INTEGER_LITERAL ->
                    LiteralExpression.create(Integer.parseInt(symbol.getText()));
                case AlertExpressionParser.PERCENTAGE_LITERAL ->
                    LiteralExpression.create(new HumanReadablePercentage(symbol.getText()));
                case AlertExpressionParser.STRING_LITERAL -> LiteralExpression.create(getUnQuotedString(symbol));
                case AlertExpressionParser.NULL_LITERAL -> null;
                case AlertExpressionParser.SIZE_LITERAL ->
                    LiteralExpression.create(HumanReadableSize.of(symbol.getText()));
                default -> throw new RuntimeException("Unsupported terminal type");
            };
        }

        static BigDecimal parseDecimal(String text) {
            return new BigDecimal(text);
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
    }
}
