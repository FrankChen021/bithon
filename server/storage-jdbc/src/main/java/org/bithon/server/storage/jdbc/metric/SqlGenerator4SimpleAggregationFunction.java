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
import org.bithon.server.storage.datasource.query.ast.ISimpleAggregateFunctionVisitor;
import org.bithon.server.storage.datasource.query.ast.SimpleAggregateExpressions;
import org.bithon.server.storage.jdbc.common.dialect.ISqlDialect;
import org.bithon.server.storage.metrics.Interval;

/**
 * @author Frank Chen
 * @date 1/11/21 3:11 pm
 */
public class SqlGenerator4SimpleAggregationFunction implements ISimpleAggregateFunctionVisitor<String> {

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
    public String visit(SimpleAggregateExpressions.CardinalityAggregateExpression aggregator) {
        return StringUtils.format("count(DISTINCT \"%s\")", aggregator.getTargetColumn());
    }

    @Override
    public String visit(SimpleAggregateExpressions.SumAggregateExpression aggregator) {
        return StringUtils.format("sum(\"%s\")", aggregator.getTargetColumn());
    }

    @Override
    public String visit(SimpleAggregateExpressions.GroupConcatAggregateExpression aggregator) {
        // No need to pass hasAlias because this type of field can't be on an expression as of now
        return sqlDialect.stringAggregator(aggregator.getTargetColumn());
    }

    @Override
    public String visit(SimpleAggregateExpressions.CountAggregateExpression aggregator) {
        return "count(1)";
    }

    @Override
    public String visit(SimpleAggregateExpressions.AvgAggregateExpression aggregator) {
        return StringUtils.format("avg(\"%s\")", aggregator.getTargetColumn());
    }

    @Override
    public String visit(SimpleAggregateExpressions.FirstAggregateExpression aggregator) {
        throw new RuntimeException("first agg not supported now");
    }

    @Override
    public String visit(SimpleAggregateExpressions.LastAggregateExpression aggregator) {
        return sqlDialect.lastAggregator(aggregator.getTargetColumn(), windowFunctionLength);
    }

    @Override
    public String visit(SimpleAggregateExpressions.RateAggregateExpression aggregator) {
        return StringUtils.format("sum(\"%s\")/%d", aggregator.getTargetColumn(), step);
    }

    @Override
    public String visit(SimpleAggregateExpressions.MaxAggregateExpression aggregator) {
        return StringUtils.format("max(\"%s\")", aggregator.getTargetColumn());
    }

    @Override
    public String visit(SimpleAggregateExpressions.MinAggregateExpression aggregator) {
        return StringUtils.format("min(\"%s\")", aggregator.getTargetColumn());
    }
}
