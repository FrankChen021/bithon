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

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.bithon.component.commons.expression.ArithmeticExpression;
import org.bithon.component.commons.expression.ComparisonExpression;
import org.bithon.component.commons.expression.ConditionalExpression;
import org.bithon.component.commons.expression.ExpressionList;
import org.bithon.component.commons.expression.IDataType;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.IdentifierExpression;
import org.bithon.component.commons.expression.LiteralExpression;
import org.bithon.component.commons.expression.LogicalExpression;
import org.bithon.component.commons.expression.expt.InvalidExpressionException;
import org.bithon.component.commons.utils.HumanReadableDuration;
import org.bithon.component.commons.utils.HumanReadableNumber;
import org.bithon.component.commons.utils.HumanReadablePercentage;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.commons.antlr4.SyntaxErrorListener;
import org.bithon.server.web.service.datasource.api.QueryField;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Set;
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
        lexer.addErrorListener(SyntaxErrorListener.of(expression));
        CommonTokenStream tokens = new CommonTokenStream(lexer);

        MetricExpressionParser parser = new MetricExpressionParser(tokens);
        parser.getErrorListeners().clear();
        parser.addErrorListener(SyntaxErrorListener.of(expression));

        MetricExpressionParser.MetricExpressionContext ctx = parser.metricExpression();
        Token last = tokens.LT(1);
        if (last.getType() != MetricExpressionParser.EOF) {
            throw InvalidExpressionException.format(expression,
                                                    last.getStartIndex(),
                                                    last.getStopIndex(),
                                                    last.getLine(),
                                                    last.getStartIndex(),
                                                    "Unexpected token");
        }

        return build(ctx);
    }

    public static IExpression build(MetricExpressionParser.MetricExpressionContext metricExpression) {
        return metricExpression.accept(new BuilderImpl());
    }

    public static IExpression build(MetricExpressionParser.AtomicMetricExpressionImplContext metricExpression) {
        return metricExpression.accept(new BuilderImpl());
    }

    private static class BuilderImpl extends MetricExpressionBaseVisitor<IExpression> {
        @Override
        public IExpression visitMetricLiteralExpression(MetricExpressionParser.MetricLiteralExpressionContext ctx) {
            return LiteralExpressionBuilder.toLiteralASTExpression(ctx.getChild(TerminalNode.class, 0).getSymbol());
        }

        @Override
        public IExpression visitArithmeticExpression(MetricExpressionParser.ArithmeticExpressionContext ctx) {
            String operator = ctx.children.get(1).getText();
            return switch (operator) {
                case "+" -> new ArithmeticExpression.ADD(ctx.getChild(0).accept(this),
                                                         ctx.getChild(2).accept(this));
                case "-" -> new ArithmeticExpression.SUB(ctx.getChild(0).accept(this),
                                                         ctx.getChild(2).accept(this));
                case "*" -> new ArithmeticExpression.MUL(ctx.getChild(0).accept(this),
                                                         ctx.getChild(2).accept(this));
                case "/" -> new ArithmeticExpression.DIV(ctx.getChild(0).accept(this),
                                                         ctx.getChild(2).accept(this));
                default -> throw new InvalidExpressionException("Unsupported arithmetic operator: %s", operator);
            };
        }

        @Override
        public IExpression visitParenthesisMetricExpression(MetricExpressionParser.ParenthesisMetricExpressionContext ctx) {
            return ctx.metricExpression().accept(this);
        }

        @Override
        public IExpression visitAtomicMetricExpression(MetricExpressionParser.AtomicMetricExpressionContext ctx) {
            return visitAtomicMetricExpressionImpl(ctx.atomicMetricExpressionImpl());
        }

        @Override
        public IExpression visitAtomicMetricExpressionImpl(MetricExpressionParser.AtomicMetricExpressionImplContext ctx) {
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

            Set<String> groupBy = null;
            MetricExpressionParser.GroupByExpressionContext groupByExpression = ctx.groupByExpression();
            if (groupByExpression != null) {
                groupBy = groupByExpression.IDENTIFIER()
                                           .stream()
                                           .map((identifier) -> identifier.getSymbol().getText())
                                           .collect(Collectors.toSet());
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
                LiteralExpression<?> expected = (LiteralExpression<?>) expectedExpression.literalExpression().accept(new LiteralExpressionBuilder());

                HumanReadableDuration offset = null;
                MetricExpressionParser.DurationExpressionContext offsetParseContext = expectedExpression.durationExpression();
                if (offsetParseContext != null) {
                    offset = offsetParseContext.accept(new DurationExpressionBuilder());
                    if (!offset.isNegative()) {
                        throw new InvalidExpressionException("The value in the offset expression '%s' must be negative.", offsetParseContext.getText());
                    }

                    if (offset.isZero()) {
                        throw new InvalidExpressionException("The value in the offset express '%s' can't be zero.", offsetParseContext.getText());
                    }

                    if (!(expected instanceof LiteralExpression.ReadablePercentageLiteral)) {
                        throw new InvalidExpressionException("The absolute value in the offset expression '%s' is not supported. ONLY percentage is supported now.", expectedExpression.literalExpression().getText());
                    }
                }

                PredicateEnum predicate = getPredicate(predicateExpression.getChild(TerminalNode.class, 0).getSymbol().getType(),
                                                       expected,
                                                       offset);

                expression.setPredicate(predicate);
                expression.setExpected(expected);
                expression.setOffset(offset);
            }

            return expression;
        }

        private static PredicateEnum getPredicate(int predicateToken,
                                                  LiteralExpression<?> expected,
                                                  HumanReadableDuration expectedWindow) {
            switch (predicateToken) {
                case MetricExpressionParser.LT:
                    checkIfFalse(expected instanceof LiteralExpression.NullLiteral, "null is not allowed after predicate '<'.");
                    return PredicateEnum.LT;

                case MetricExpressionParser.LTE:
                    checkIfFalse(expected instanceof LiteralExpression.NullLiteral, "null is not allowed after predicate '<='.");
                    return PredicateEnum.LTE;

                case MetricExpressionParser.GT:
                    checkIfFalse(expected instanceof LiteralExpression.NullLiteral, "null is not allowed after predicate '>'.");
                    return PredicateEnum.GT;

                case MetricExpressionParser.GTE:
                    checkIfFalse(expected instanceof LiteralExpression.NullLiteral, "null is not allowed after predicate '>='.");
                    return PredicateEnum.GTE;

                case MetricExpressionParser.NE:
                    checkIfFalse(expected instanceof LiteralExpression.NullLiteral, "null is not allowed after predicate '<>'.");
                    checkIfTrue(expectedWindow == null, "<> is not allowed for relative comparison.");
                    return PredicateEnum.NE;

                case MetricExpressionParser.EQ:
                    checkIfFalse(expected instanceof LiteralExpression.NullLiteral, "null is not allowed after predicate '='.");
                    checkIfTrue(expectedWindow == null, "= is not allowed for relative comparison.");
                    return PredicateEnum.EQ;

                case MetricExpressionParser.IS:
                    checkIfTrue(expected instanceof LiteralExpression.NullLiteral, "Only 'null' is allowed after the 'is' predicate.");
                    checkIfTrue(expectedWindow == null, "The expected window must be NULL if the 'is' operator is used.");
                    return PredicateEnum.IS_NULL;

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
                case MetricExpressionParser.HASTOKEN -> {
                    checkIfTrue(expected instanceof LiteralExpression.StringLiteral, "The expected value of 'hasToken' predicate must be type of string literal.");
                    yield new ConditionalExpression.HasToken(identifier, expected);
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

        public static LiteralExpression<?> toLiteralASTExpression(Token symbol) {
            return switch (symbol.getType()) {
                case MetricExpressionParser.DECIMAL_LITERAL -> LiteralExpression.ofDecimal(parseDecimal(symbol.getText()));
                case MetricExpressionParser.INTEGER_LITERAL -> LiteralExpression.ofLong(Integer.parseInt(symbol.getText()));
                case MetricExpressionParser.PERCENTAGE_LITERAL -> LiteralExpression.of(new HumanReadablePercentage(symbol.getText()));
                case MetricExpressionParser.STRING_LITERAL -> {
                    String input = symbol.getText();
                    if (!input.isEmpty()) {
                        char quote = input.charAt(0);
                        input = input.substring(1, input.length() - 1);

                        if (quote == '\'') {
                            input = StringUtils.unEscape(input, '\\', '\'');
                        } else {
                            input = StringUtils.unEscape(input, '\\', '\"');
                        }
                    }
                    yield LiteralExpression.ofString(input);
                }
                case MetricExpressionParser.NULL_LITERAL -> LiteralExpression.NullLiteral.INSTANCE;
                case MetricExpressionParser.SIZE_LITERAL -> LiteralExpression.of(HumanReadableNumber.of(symbol.getText()));
                case MetricExpressionParser.DURATION_LITERAL -> LiteralExpression.of(HumanReadableDuration.parse(symbol.getText()));

                default -> throw new RuntimeException("Unsupported terminal type");
            };
        }

        @Override
        public IExpression visitLiteralExpression(MetricExpressionParser.LiteralExpressionContext ctx) {
            Token symbol = ctx.getChild(TerminalNode.class, 0).getSymbol();
            return toLiteralASTExpression(symbol);
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
    }
}
