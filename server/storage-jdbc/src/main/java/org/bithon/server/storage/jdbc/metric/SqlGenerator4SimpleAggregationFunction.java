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

import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.storage.datasource.query.ast.IQueryAggregateFunctionVisitor;
import org.bithon.server.storage.datasource.query.ast.QueryAggregateFunctions;
import org.bithon.server.storage.jdbc.common.dialect.ISqlDialect;
import org.bithon.server.storage.metrics.Interval;

/**
 * @author Frank Chen
 * @date 1/11/21 3:11 pm
 */
public class SqlGenerator4SimpleAggregationFunction implements IQueryAggregateFunctionVisitor<String> {

    private final ISqlDialect sqlDialect;

    /**
     * in second
     */
    private final int step;
    private long windowFunctionLength;

    public SqlGenerator4SimpleAggregationFunction(ISqlDialect sqlDialect, Interval interval) {
        this.sqlDialect = sqlDialect;

        if (interval.getStep() != null) {
            windowFunctionLength = interval.getStep();
            step = interval.getStep();
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

    @Override
    public String visit(QueryAggregateFunctions.CardinalityAggregateExpression aggregator) {
        return StringUtils.format("count(DISTINCT %s)", sqlDialect.quoteIdentifier(aggregator.getTargetColumn()));
    }

    @Override
    public String visit(QueryAggregateFunctions.SumAggregateExpression aggregator) {
        return StringUtils.format("sum(%s)", sqlDialect.quoteIdentifier(aggregator.getTargetColumn()));
    }

    @Override
    public String visit(QueryAggregateFunctions.GroupConcatAggregateExpression aggregator) {
        // No need to pass hasAlias because this type of field can't be on an expression as of now
        return sqlDialect.stringAggregator(aggregator.getTargetColumn());
    }

    @Override
    public String visit(QueryAggregateFunctions.CountAggregateExpression aggregator) {
        return "count(1)";
    }

    @Override
    public String visit(QueryAggregateFunctions.AvgAggregateExpression aggregator) {
        return StringUtils.format("avg(%s)", sqlDialect.quoteIdentifier(aggregator.getTargetColumn()));
    }

    @Override
    public String visit(QueryAggregateFunctions.FirstAggregateExpression aggregator) {
        throw new RuntimeException("first agg not supported now");
    }

    @Override
    public String visit(QueryAggregateFunctions.LastAggregateExpression aggregator) {
        return sqlDialect.lastAggregator(aggregator.getTargetColumn(), windowFunctionLength);
    }

    @Override
    public String visit(QueryAggregateFunctions.RateAggregateExpression aggregator) {
        return StringUtils.format("sum(%s)/%d", sqlDialect.quoteIdentifier(aggregator.getTargetColumn()), step);
    }

    @Override
    public String visit(QueryAggregateFunctions.MaxAggregateExpression aggregator) {
        return StringUtils.format("max(%s)", sqlDialect.quoteIdentifier(aggregator.getTargetColumn()));
    }

    @Override
    public String visit(QueryAggregateFunctions.MinAggregateExpression aggregator) {
        return StringUtils.format("min(%s)", sqlDialect.quoteIdentifier(aggregator.getTargetColumn()));
    }
}
