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

package org.bithon.server.metric.expression.ast;


import org.bithon.component.commons.expression.ArithmeticExpression;
import org.bithon.component.commons.expression.ComparisonExpression;
import org.bithon.component.commons.expression.ConditionalExpression;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.LiteralExpression;
import org.bithon.component.commons.expression.optimzer.AbstractOptimizer;
import org.bithon.component.commons.expression.optimzer.ConstantFoldingOptimizer;

/**
 * @author frank.chen021@outlook.com
 * @date 6/4/25 2:58 pm
 */
public class MetricExpressionOptimizer {

    static class ExtendedConstantFoldingOptimizer
        extends ConstantFoldingOptimizer
        implements IMetricExpressionVisitor<IExpression> {

        @Override
        public IExpression visit(MetricAggregateExpression expression) {
            return expression;
        }

        @Override
        public IExpression visit(MetricSelectExpression expression) {
            return expression;
        }

        @Override
        public IExpression visit(MetricExpectedExpression expression) {
            return expression;
        }
    }

    public static IExpression optimize(IExpression expression) {
        return expression.accept(new ExtendedConstantFoldingOptimizer())
                         //.accept(new OperatorPushingOptimizer())
            ;
    }

    /**
     * for comparison expression: sum(metric) > 5,
     * it's parsed as:
     * <pre>
     *     ComparisonExpression
     *     ├── MetricExpression
     *     └── ConstantExpression
     * </pre>
     * We can push down the predicate expression and constant expression to the metric expression as
     * <pre>
     *     MetricExpression
     *     └── ExpectedExpression(ConstantExpression)
     * </pre>
     * <p>
     * for arithmetic expression: sum(metric) + 5,
     * it's parsed as:
     * <pre>
     *     ArithmeticExpression
     *     ├── MetricExpression
     *     └── ConstantExpression
     *     </pre>
     * We can push down the constant expression to the metric expression as
     * <pre>
     *     MetricExpression
     *     └── PostExpression(ConstantExpression)
     *  </pre>
     */
    public static class OperatorPushingOptimizer extends AbstractOptimizer implements IMetricExpressionVisitor<IExpression> {
        public static IExpression optimize(IExpression expression) {
            return expression.accept(new OperatorPushingOptimizer());
        }

        private OperatorPushingOptimizer() {
        }

        @Override
        public IExpression visit(MetricAggregateExpression expression) {
            return expression;
        }

        @Override
        public IExpression visit(ConditionalExpression expression) {
            IExpression lhs = expression.getLhs().accept(this);
            IExpression rhs = expression.getRhs().accept(this);

            if (lhs instanceof MetricAggregateExpression metricExpression
                && rhs instanceof LiteralExpression<?> expectedExpression) {
                metricExpression.setExpected(expectedExpression);
                if (expression instanceof ComparisonExpression.LT) {
                    metricExpression.setPredicate(PredicateEnum.LT);
                } else if (expression instanceof ComparisonExpression.GT) {
                    metricExpression.setPredicate(PredicateEnum.GT);
                } else if (expression instanceof ComparisonExpression.LTE) {
                    metricExpression.setPredicate(PredicateEnum.LTE);
                } else if (expression instanceof ComparisonExpression.GTE) {
                    metricExpression.setPredicate(PredicateEnum.GTE);
                } else if (expression instanceof ComparisonExpression.EQ) {
                    metricExpression.setPredicate(PredicateEnum.EQ);
                } else if (expression instanceof ComparisonExpression.NE) {
                    metricExpression.setPredicate(PredicateEnum.NE);
                } else {
                    throw new UnsupportedOperationException("Unsupported comparison expression: " + expression.getClass().getSimpleName());
                }
                return metricExpression;
            }

            return expression;
        }

        /**
         * for expression like: sum(metric) + sum(metric2), apply optimization if:
         * 1. they're on the same data source with same filters and same group-by,
         * 2. the underlying data source supports multiple metrics in a single query
         * then we can merge them into a single metric expression
         */
        @Override
        public IExpression visit(ArithmeticExpression expression) {
            return super.visit(expression);
        }
    }
}
