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

package org.bithon.server.datasource.reader.clickhouse.expression;

import org.bithon.component.commons.expression.ConditionalExpression;
import org.bithon.component.commons.expression.FunctionExpression;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.IdentifierExpression;
import org.bithon.component.commons.expression.function.builtin.AggregateFunction;
import org.bithon.component.commons.expression.function.builtin.StringFunction;
import org.bithon.component.commons.expression.optimzer.AbstractOptimizer;
import org.bithon.server.datasource.ISchema;
import org.bithon.server.datasource.column.IColumn;
import org.bithon.server.datasource.reader.clickhouse.AggregateFunctionColumn;

import java.util.List;

/**
 * @author frank chen
 * @date 02/05/24 22:11pm
 */
public class ClickHouseExpressionOptimizer extends AbstractOptimizer {
    private final ISchema schema;

    public ClickHouseExpressionOptimizer() {
        this.schema = null;
    }

    public ClickHouseExpressionOptimizer(ISchema schema) {
        this.schema = schema;
    }

    @Override
    public IExpression visit(ConditionalExpression expression) {
        if (expression instanceof ConditionalExpression.StartsWith) {
            return new FunctionExpression(
                StringFunction.StartsWith.INSTANCE,
                expression.getLhs(),
                expression.getRhs()
            );
        }

        if (expression instanceof ConditionalExpression.EndsWith) {
            return new FunctionExpression(
                StringFunction.EndsWith.INSTANCE,
                expression.getLhs(),
                expression.getRhs()
            );
        }

        if (expression instanceof ConditionalExpression.RegularExpressionMatchExpression regularExpressionMatchExpression) {
            return RegularExpressionMatchOptimizer.optimize(regularExpressionMatchExpression);
        }

        if (expression instanceof ConditionalExpression.HasToken) {
            return this.visit(new FunctionExpression(StringFunction.HasToken.INSTANCE, expression.getLhs(), expression.getRhs()));
        }

        return super.visit(expression);
    }

    @Override
    public IExpression visit(FunctionExpression expression) {

        // Try to replace the aggregator on AggregateFunction column to sumMerge
        if (schema != null) {
            if (expression.getFunction() instanceof AggregateFunction.Sum) {
                IExpression inputArgs = expression.getArgs().get(0);
                if (inputArgs instanceof IdentifierExpression identifier) {
                    IColumn column = schema.getColumnByName(identifier.getIdentifier());
                    if (column instanceof AggregateFunctionColumn) {
                        return new FunctionExpression(
                            AggregateFunctionColumn.SumMergeFunction.INSTANCE,
                            identifier
                        );
                    }
                }
            } else if (expression.getFunction() instanceof AggregateFunction.Count
                       // 'count' aggregator can accept zero argument
                       && !expression.getArgs().isEmpty()) {
                IExpression inputArgs = expression.getArgs().get(0);
                if (inputArgs instanceof IdentifierExpression identifier) {
                    IColumn column = schema.getColumnByName(identifier.getIdentifier());
                    if (column instanceof AggregateFunctionColumn) {
                        return new FunctionExpression(
                            AggregateFunctionColumn.CountMergeFunction.INSTANCE,
                            identifier
                        );
                    }
                }
            }
        }

        if (expression.getFunction() instanceof StringFunction.HasToken) {
            // Apply the optimization for hasToken function
            return HasTokenFunctionOptimizer.optimize(expression);
        }

        if (expression.getFunction() instanceof AggregateFunction.First) {
            return new FunctionExpression(new ArgMinFunction(), expression.getArgs().get(0), IdentifierExpression.of("timestamp"));
        }

        if (expression.getFunction() instanceof AggregateFunction.Last) {
            return new FunctionExpression(new ArgMaxFunction(), expression.getArgs().get(0), IdentifierExpression.of("timestamp"));
        }

        return super.visit(expression);
    }

    static class ArgMinFunction extends AggregateFunction {
        public ArgMinFunction() {
            super("argMin");
        }

        @Override
        public void validateArgs(List<IExpression> args) {
            validateTrue(args.size() == 2, "Function [argMin] accepts 2 parameters, but got [%d]", args.size());
        }

        @Override
        public Object evaluate(List<Object> args) {
            throw new UnsupportedOperationException();
        }
    }

    static class ArgMaxFunction extends AggregateFunction {
        public ArgMaxFunction() {
            super("argMax");
        }

        @Override
        public void validateArgs(List<IExpression> args) {
            validateTrue(args.size() == 2, "Function [argMax] accepts 2 parameters, but got [%d]", args.size());
        }

        @Override
        public Object evaluate(List<Object> args) {
            throw new UnsupportedOperationException();
        }
    }

}
