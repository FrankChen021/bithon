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

package org.bithon.server.metric.expression;

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
import org.bithon.server.alerting.common.parser.MetricExpressionBaseVisitor;
import org.bithon.server.alerting.common.parser.MetricExpressionLexer;
import org.bithon.server.alerting.common.parser.MetricExpressionParser;
import org.bithon.server.web.service.datasource.api.QueryField;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 2024/1/7 18:18
 */
public class MetricExpressionASTBuilder {

    static void checkIfTrue(boolean shouldBeTrue, String message) {
        if (!shouldBeTrue) {
            throw new InvalidExpressionException(message);
        }
    }

    static void checkIfFalse(boolean shouldBeFalse, String message) {
        if (shouldBeFalse) {
            throw new InvalidExpressionException(message);
        }
    }

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

        MetricExpressionParser.MetricExpressionContext ctx = parser.metricExpression();
        if (tokens.LT(1).getType() != MetricExpressionParser.EOF) {
            throw new InvalidExpressionException(expression, tokens.LT(1).getStartIndex(), "Unexpected token");
        }

        return build(ctx);
    }

    public static MetricExpression build(MetricExpressionParser.MetricExpressionContext metricExpression) {
        return metricExpression.accept(new BuilderImpl());
    }

    private static class BuilderImpl extends MetricExpressionBaseVisitor<MetricExpression> {
        @Override
        public MetricExpression visitMetricExpression(MetricExpressionParser.MetricExpressionContext ctx) {
            String[] names = ctx.metricQNameExpression().getText().split("\\.");
            String from = names[0];
            String metric = names[1];

            String aggregatorText = ctx.aggregatorExpression().getText().toLowerCase(Locale.ENGLISH);
            AggregatorEnum aggregator;
            try {
                aggregator = AggregatorEnum.valueOf(aggregatorText);
            } catch (RuntimeException ignored) {
                throw new InvalidExpressionException(StringUtils.format("The aggregator [%s] in the expression is not supported", aggregatorText));
            }

            HumanReadableDuration duration = null;
            MetricExpressionParser.DurationExpressionContext windowExpressionCtx = ctx.durationExpression();
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
            MetricExpressionParser.LabelExpressionContext where = ctx.labelExpression();
            if (where != null) {
                LabelSelectorExpressionBuilder filterASTBuilder = new LabelSelectorExpressionBuilder();
                List<IExpression> filters = where.labelSelectorExpression()
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
            MetricExpressionParser.GroupByExpressionContext groupByExpression = ctx.groupByExpression();
            if (groupByExpression != null) {
                groupBy = groupByExpression.IDENTIFIER()
                                           .stream()
                                           .map((identifier) -> identifier.getSymbol().getText())
                                           .collect(Collectors.toList());
            }

            MetricExpression expression = new MetricExpression();
            expression.setFrom(from);
            expression.setLabelSelectorExpression(whereExpression);

            // For 'count' aggregator, use the 'count' as output column instead of using the column name as output name
            expression.setMetric(new QueryField(aggregator.equals(AggregatorEnum.count) ? "count" : metric, metric, aggregator.name()));
            expression.setGroupBy(groupBy);
            expression.setWindow(duration);

            //
            // Metric Predicate
            //
            MetricExpressionParser.MetricPredicateExpressionContext predicateExpression = ctx.metricPredicateExpression();
            if (predicateExpression != null) {
                MetricExpressionParser.MetricExpectedExpressionContext expectedExpression = ctx.metricExpectedExpression();
                LiteralExpression expected = (LiteralExpression) expectedExpression.literalExpression().accept(new LiteralExpressionBuilder());

                HumanReadableDuration expectedWindow = null;
                MetricExpressionParser.DurationExpressionContext expectedWindowCtx = expectedExpression.durationExpression();
                if (expectedWindowCtx != null) {
                    expectedWindow = expectedWindowCtx.accept(new DurationExpressionBuilder());
                    if (!expectedWindow.isNegative()) {
                        throw new InvalidExpressionException("The value in the expected window expression '%s' must be a negative value.", expectedWindowCtx.getText());
                    }

                    if (expectedWindow.isZero()) {
                        throw new InvalidExpressionException("The value in the expected window expression '%s' can't be zero.", expectedWindowCtx.getText());
                    }
                }

                MetricExpression.Predicate predicate = getPredicate(predicateExpression.getChild(TerminalNode.class, 0).getSymbol().getType(),
                                                                    expected,
                                                                    expectedWindow);

                expression.setPredicate(predicate);
                expression.setExpected(expected);
                expression.setExpectedWindow(expectedWindow);
            }

            return expression;
        }

        private static MetricExpression.Predicate getPredicate(int predicateToken,
                                                               LiteralExpression expected,
                                                               HumanReadableDuration expectedWindow) {
            switch (predicateToken) {
                case MetricExpressionParser.LT:
                    checkIfFalse(expected instanceof LiteralExpression.NullLiteral, "null is not allowed after predicate '<'.");
                    return MetricExpression.Predicate.LT;

                case MetricExpressionParser.LTE:
                    checkIfFalse(expected instanceof LiteralExpression.NullLiteral, "null is not allowed after predicate '<='.");
                    return MetricExpression.Predicate.LTE;

                case MetricExpressionParser.GT:
                    checkIfFalse(expected instanceof LiteralExpression.NullLiteral, "null is not allowed after predicate '>'.");
                    return MetricExpression.Predicate.GT;

                case MetricExpressionParser.GTE:
                    checkIfFalse(expected instanceof LiteralExpression.NullLiteral, "null is not allowed after predicate '>='.");
                    return MetricExpression.Predicate.GTE;

                case MetricExpressionParser.NE:
                    checkIfFalse(expected instanceof LiteralExpression.NullLiteral, "null is not allowed after predicate '<>'.");
                    checkIfTrue(expectedWindow == null, "<> is not allowed for relative comparison.");
                    return MetricExpression.Predicate.NE;

                case MetricExpressionParser.EQ:
                    checkIfFalse(expected instanceof LiteralExpression.NullLiteral, "null is not allowed after predicate '='.");
                    checkIfTrue(expectedWindow == null, "= is not allowed for relative comparison.");
                    return MetricExpression.Predicate.EQ;

                case MetricExpressionParser.IS:
                    checkIfTrue(expected instanceof LiteralExpression.NullLiteral, "Only 'null' is allowed after the 'is' predicate.");
                    checkIfTrue(expectedWindow == null, "The expected window must be NULL if the 'is' operator is used.");
                    return MetricExpression.Predicate.IS_NULL;

                default:
                    throw new RuntimeException("Unsupported alert predicate");
            }
        }
    }

    private static class DurationExpressionBuilder extends MetricExpressionBaseVisitor<HumanReadableDuration> {
        @Override
        public HumanReadableDuration visitDurationExpression(MetricExpressionParser.DurationExpressionContext ctx) {
            TerminalNode duration = (TerminalNode) ctx.getChild(1);
            return HumanReadableDuration.parse(duration.getText());
        }
    }

    private static class LabelSelectorExpressionBuilder extends MetricExpressionBaseVisitor<IExpression> {

        @Override
        public IExpression visitComparisonExpression(MetricExpressionParser.ComparisonExpressionContext ctx) {
            IdentifierExpression identifier = new IdentifierExpression(ctx.IDENTIFIER().getSymbol().getText());
            IExpression expected = ctx.literalExpression().accept(new LiteralExpressionBuilder());

            TerminalNode predicate;
            boolean isNotExpression = false;
            MetricExpressionParser.LabelPredicateExpressionContext predicateContext = ctx.labelPredicateExpression();
            if (predicateContext.getChildCount() == 2) {
                isNotExpression = true;
                predicate = predicateContext.getChild(TerminalNode.class, 1);
            } else {
                predicate = predicateContext.getChild(TerminalNode.class, 0);
            }

            IExpression expression = createSelectorExpression(predicate.getSymbol().getType(), identifier, expected);
            return isNotExpression ? new LogicalExpression.NOT(expression) : expression;
        }

        private IExpression createSelectorExpression(int predicateType, IdentifierExpression identifier, IExpression expected) {
            return switch (predicateType) {
                case MetricExpressionParser.LT -> {
                    checkIfTrue(expected instanceof LiteralExpression, "The expected value of '<' predicate must be type of literal.");
                    yield new ComparisonExpression.LT(identifier, expected);
                }
                case MetricExpressionParser.LTE -> {
                    checkIfTrue(expected instanceof LiteralExpression, "The expected value of '<=' predicate must be type of literal.");
                    yield new ComparisonExpression.LTE(identifier, expected);
                }
                case MetricExpressionParser.GT -> {
                    checkIfTrue(expected instanceof LiteralExpression, "The expected value of '>' predicate must be type of literal.");
                    yield new ComparisonExpression.GT(identifier, expected);
                }
                case MetricExpressionParser.GTE -> {
                    checkIfTrue(expected instanceof LiteralExpression, "The expected value of '>=' predicate must be type of literal.");
                    yield new ComparisonExpression.GTE(identifier, expected);
                }
                case MetricExpressionParser.NE -> {
                    checkIfTrue(expected instanceof LiteralExpression, "The expected value of '<>' predicate must be type of literal.");
                    yield new ComparisonExpression.NE(identifier, expected);
                }
                case MetricExpressionParser.EQ -> {
                    checkIfTrue(expected instanceof LiteralExpression, "The expected value of '=' predicate must be type of literal.");
                    yield new ComparisonExpression.EQ(identifier, expected);
                }
                case MetricExpressionParser.CONTAINS -> {
                    checkIfTrue(expected instanceof LiteralExpression.StringLiteral, "The expected value of 'contains' predicate must be type of string literal.");
                    yield new ConditionalExpression.Contains(identifier, expected);
                }
                case MetricExpressionParser.STARTSWITH -> {
                    checkIfTrue(expected instanceof LiteralExpression.StringLiteral, "The expected value of 'startsWith' predicate must be type of string literal.");
                    yield new ConditionalExpression.StartsWith(identifier, expected);
                }
                case MetricExpressionParser.ENDSWITH -> {
                    checkIfTrue(expected instanceof LiteralExpression.StringLiteral, "The expected value of 'endsWith' predicate must be type of string literal.");
                    yield new ConditionalExpression.EndsWith(identifier, expected);
                }
                // For compatibility
                case MetricExpressionParser.LIKE -> {
                    checkIfTrue(expected instanceof LiteralExpression.StringLiteral, "The expected value of 'LIKE' predicate must be type of string literal.");
                    yield new ConditionalExpression.Like(identifier, expected);
                }
                default -> throw new RuntimeException("Unsupported predicate type: " + predicateType);
            };
        }

        @Override
        public IExpression visitInExpression(MetricExpressionParser.InExpressionContext ctx) {
            IdentifierExpression identifier = new IdentifierExpression(ctx.IDENTIFIER().getSymbol().getText());
            ExpressionList expected = (ExpressionList) ctx.literalListExpression().accept(new LiteralExpressionBuilder());
            if (ctx.getChildCount() == 4) {
                return new ConditionalExpression.NotIn(identifier, expected);
            } else {
                return new ConditionalExpression.In(identifier, expected);
            }
        }
    }

    private static class LiteralExpressionBuilder extends MetricExpressionBaseVisitor<IExpression> {

        @Override
        public IExpression visitLiteralExpression(MetricExpressionParser.LiteralExpressionContext ctx) {
            Token symbol = ctx.getChild(TerminalNode.class, 0).getSymbol();
            return switch (symbol.getType()) {
                case MetricExpressionParser.DECIMAL_LITERAL -> LiteralExpression.create(parseDecimal(symbol.getText()));
                case MetricExpressionParser.INTEGER_LITERAL ->
                    LiteralExpression.create(Integer.parseInt(symbol.getText()));
                case MetricExpressionParser.PERCENTAGE_LITERAL ->
                    LiteralExpression.create(new HumanReadablePercentage(symbol.getText()));
                case MetricExpressionParser.STRING_LITERAL -> LiteralExpression.create(getUnQuotedString(symbol));
                case MetricExpressionParser.NULL_LITERAL -> LiteralExpression.NullLiteral.INSTANCE;
                case MetricExpressionParser.SIZE_LITERAL ->
                    LiteralExpression.create(HumanReadableSize.of(symbol.getText()));
                default -> throw new RuntimeException("Unsupported terminal type");
            };
        }

        @Override
        public IExpression visitLiteralListExpression(MetricExpressionParser.LiteralListExpressionContext ctx) {
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
