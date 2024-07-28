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

package org.bithon.server.storage.jdbc.metric;

import org.bithon.component.commons.expression.FunctionExpression;
import org.bithon.component.commons.expression.IdentifierExpression;
import org.bithon.component.commons.expression.function.IFunction;
import org.bithon.component.commons.expression.function.builtin.AggregateFunction;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.storage.datasource.query.ast.Function;
import org.bithon.server.storage.jdbc.common.dialect.ISqlDialect;
import org.bithon.server.storage.metrics.Interval;

/**
 * @author Frank Chen
 * @date 1/11/21 3:11 pm
 */
public class SqlGenerator4SimpleAggregationFunction {

    private final ISqlDialect sqlDialect;

    /**
     * in second
     */
    private final long step;
    private long windowFunctionLength;

    public SqlGenerator4SimpleAggregationFunction(ISqlDialect sqlDialect, Interval interval) {
        this.sqlDialect = sqlDialect;

        if (interval.getStep() != null) {
            windowFunctionLength = interval.getStep().getSeconds();
            step = interval.getStep().getSeconds();
        } else {
            /**
             * For Window functions, since the timestamp of records might cross two windows,
             * we need to make sure the record in the given time range has only one window.
             */
            long endTime = interval.getEndTime().getMilliseconds();
            long startTime = interval.getStartTime().getMilliseconds();
            windowFunctionLength = (endTime - startTime) / 1000;
            while (startTime / windowFunctionLength != endTime / windowFunctionLength) {
                windowFunctionLength *= 2;
            }

            step = interval.getTotalLength();
        }
    }

    public String generate(FunctionExpression function) {
        IFunction underlyingFunction = function.getFunction();
        String field = ((IdentifierExpression) function.getArgs().get(0)).getIdentifier();
        if (underlyingFunction instanceof AggregateFunction.Sum) {
            return StringUtils.format("sum(%s)", sqlDialect.quoteIdentifier(field));
        }
        if (underlyingFunction instanceof Function.Cardinality) {
            return StringUtils.format("count(DISTINCT %s)", sqlDialect.quoteIdentifier(field));
        }
        if (underlyingFunction instanceof Function.GroupConcat) {
            // No need to pass hasAlias because this type of field can't be on an expression as of now
            return sqlDialect.stringAggregator(field);
        }
        if (underlyingFunction instanceof AggregateFunction.Count) {
            return "count(1)";
        }
        if (underlyingFunction instanceof AggregateFunction.Avg) {
            return StringUtils.format("avg(%s)", sqlDialect.quoteIdentifier(field));
        }
        if (underlyingFunction instanceof AggregateFunction.First) {
            throw new RuntimeException("first agg not supported now");
        }
        if (underlyingFunction instanceof AggregateFunction.Last) {
            return sqlDialect.lastAggregator(field, windowFunctionLength);
        }
        if (underlyingFunction instanceof Function.Rate) {
            return StringUtils.format("sum(%s)/%d", sqlDialect.quoteIdentifier(field), step);
        }
        if (underlyingFunction instanceof AggregateFunction.Max) {
            return StringUtils.format("max(%s)", sqlDialect.quoteIdentifier(field));
        }
        if (underlyingFunction instanceof AggregateFunction.Min) {
            return StringUtils.format("min(%s)", sqlDialect.quoteIdentifier(field));
        }

        throw new RuntimeException("Not support function: " + underlyingFunction.getName());
    }
}
