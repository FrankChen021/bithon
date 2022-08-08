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
import org.bithon.server.storage.datasource.api.GroupConcatAggregator;
import org.bithon.server.storage.datasource.api.IQueryStageAggregatorVisitor;
import org.bithon.server.storage.datasource.api.QueryStageAggregators;

/**
 * @author Frank Chen
 * @date 1/11/21 3:11 pm
 */
public class QueryStageAggregatorSQLGenerator implements IQueryStageAggregatorVisitor<String> {

    private final ISqlExpressionFormatter formatter;

    /**
     * in second
     */
    private final long interval;

    public QueryStageAggregatorSQLGenerator(ISqlExpressionFormatter sqlFormatter, long interval) {
        this.interval = interval;
        this.formatter = sqlFormatter;
    }

    @Override
    public String visit(QueryStageAggregators.CardinalityAggregator aggregator) {
        return StringUtils.format("count(DISTINCT \"%s\") AS \"%s\"", aggregator.getDimension(), aggregator.getName());
    }

    @Override
    public String visit(QueryStageAggregators.SumAggregator aggregator) {
        return StringUtils.format("sum(\"%s\") AS \"%s\"", aggregator.getName(), aggregator.getName());
    }

    @Override
    public String visit(GroupConcatAggregator groupConcatAggregator) {
        return formatter.stringAggregator(groupConcatAggregator.getField(), groupConcatAggregator.getName());
    }

    @Override
    public String visit(QueryStageAggregators.CountAggregator aggregator) {
        return StringUtils.format("count(\"%s\") AS \"%s\"", aggregator.getName(), aggregator.getName());
    }

    @Override
    public String visit(QueryStageAggregators.AvgAggregator aggregator) {
        return StringUtils.format("avg(\"%s\") AS \"%s\"", aggregator.getName(), aggregator.getName());
    }

    @Override
    public String visit(QueryStageAggregators.FirstAggregator aggregator) {
        return null;
    }

    @Override
    public String visit(QueryStageAggregators.LastAggregator aggregator) {
        return null;
    }

    @Override
    public String visit(QueryStageAggregators.RateAggregator aggregator) {
        return StringUtils.format("sum(\"%s\")/%d AS \"%s\"", aggregator.getName(), interval, aggregator.getName());
    }

    @Override
    public String visit(QueryStageAggregators.MaxAggregator aggregator) {
        return StringUtils.format("max(\"%s\") AS \"%s\"", aggregator.getName(), aggregator.getName());
    }

    @Override
    public String visit(QueryStageAggregators.MinAggregator aggregator) {
        return StringUtils.format("min(\"%s\") AS \"%s\"", aggregator.getName(), aggregator.getName());
    }
}
