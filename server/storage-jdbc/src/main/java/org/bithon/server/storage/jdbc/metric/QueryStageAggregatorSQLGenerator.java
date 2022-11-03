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
import org.bithon.server.storage.datasource.query.IQueryStageAggregatorVisitor;
import org.bithon.server.storage.datasource.query.ast.SimpleAggregators;

/**
 * @author Frank Chen
 * @date 1/11/21 3:11 pm
 */
public class QueryStageAggregatorSQLGenerator implements IQueryStageAggregatorVisitor<String> {

    private final ISqlDialect formatter;

    /**
     * in second
     */
    private final long step;
    private final long length;

    public QueryStageAggregatorSQLGenerator(ISqlDialect sqlFormatter, long length, long step) {
        this.step = step;
        this.length = length;
        this.formatter = sqlFormatter;
    }

    @Override
    public String visit(SimpleAggregators.CardinalityAggregator aggregator) {
        return StringUtils.format("count(DISTINCT \"%s\")", aggregator.getTargetField());
    }

    @Override
    public String visit(SimpleAggregators.SumAggregator aggregator) {
        return StringUtils.format("sum(\"%s\")", aggregator.getTargetField());
    }

    @Override
    public String visit(SimpleAggregators.GroupConcatAggregator aggregator) {
        // No need to pass hasAlias because this type of field can't be on an expression as of now
        return formatter.stringAggregator(aggregator.getTargetField());
    }

    @Override
    public String visit(SimpleAggregators.CountAggregator aggregator) {
        return "count(1)";
    }

    @Override
    public String visit(SimpleAggregators.AvgAggregator aggregator) {
        return StringUtils.format("avg(\"%s\")", aggregator.getTargetField());
    }

    @Override
    public String visit(SimpleAggregators.FirstAggregator aggregator) {
        throw new RuntimeException("first agg not supported now");
    }

    @Override
    public String visit(SimpleAggregators.LastAggregator aggregator) {
        return formatter.lastAggregator(aggregator.getTargetField(), step);
    }

    @Override
    public String visit(SimpleAggregators.RateAggregator aggregator) {
        return StringUtils.format("sum(\"%s\")/%d", aggregator.getTargetField(), step);
    }

    @Override
    public String visit(SimpleAggregators.MaxAggregator aggregator) {
        return StringUtils.format("max(\"%s\")", aggregator.getTargetField());
    }

    @Override
    public String visit(SimpleAggregators.MinAggregator aggregator) {
        return StringUtils.format("min(\"%s\")", aggregator.getTargetField());
    }
}
