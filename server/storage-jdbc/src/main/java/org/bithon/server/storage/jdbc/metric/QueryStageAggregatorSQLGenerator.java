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
import org.bithon.server.storage.datasource.api.IQueryStageAggregatorVisitor;
import org.bithon.server.storage.datasource.api.QueryStageAggregators;

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

    private final boolean hasAlias;

    public QueryStageAggregatorSQLGenerator(ISqlDialect sqlFormatter, long length, long step) {
        this(sqlFormatter, length, step, true);
    }

    public QueryStageAggregatorSQLGenerator(ISqlDialect sqlFormatter, long length, long step, boolean hasAlias) {
        this.step = step;
        this.length = length;
        this.formatter = sqlFormatter;
        this.hasAlias = hasAlias;
    }

    @Override
    public String visit(QueryStageAggregators.CardinalityAggregator aggregator) {
        if (hasAlias) {
            return StringUtils.format("count(DISTINCT \"%s\") AS \"%s\"", aggregator.getField(), aggregator.getName());
        } else {
            return StringUtils.format("count(DISTINCT \"%s\")", aggregator.getField());
        }
    }

    @Override
    public String visit(QueryStageAggregators.SumAggregator aggregator) {
        if (hasAlias) {
            return StringUtils.format("sum(\"%s\") AS \"%s\"", aggregator.getField(), aggregator.getName());
        } else {
            return StringUtils.format("sum(\"%s\")", aggregator.getField());
        }
    }

    @Override
    public String visit(QueryStageAggregators.GroupConcatAggregator aggregator) {
        // No need to pass hasAlias because this type of field can't be on a expression as of now
        return formatter.stringAggregator(aggregator.getField(), aggregator.getName());
    }

    @Override
    public String visit(QueryStageAggregators.CountAggregator aggregator) {
        if (hasAlias) {
            return StringUtils.format("count(1) AS \"%s\"", aggregator.getName());
        } else {
            return "count(1)";
        }
    }

    @Override
    public String visit(QueryStageAggregators.AvgAggregator aggregator) {
        if (hasAlias) {
            return StringUtils.format("avg(\"%s\") AS \"%s\"", aggregator.getField(), aggregator.getName());
        } else {
            return StringUtils.format("avg(\"%s\")", aggregator.getField());
        }
    }

    @Override
    public String visit(QueryStageAggregators.FirstAggregator aggregator) {
        throw new RuntimeException("first agg not supported now");
    }

    @Override
    public String visit(QueryStageAggregators.LastAggregator aggregator) {
        return formatter.lastAggregator(aggregator.getField(), hasAlias ? aggregator.getName() : "", step);
    }

    @Override
    public String visit(QueryStageAggregators.RateAggregator aggregator) {
        if (hasAlias) {
            return StringUtils.format("sum(\"%s\")/%d AS \"%s\"", aggregator.getField(), step, aggregator.getName());
        } else {
            return StringUtils.format("sum(\"%s\")/%d", aggregator.getField(), step);
        }
    }

    @Override
    public String visit(QueryStageAggregators.MaxAggregator aggregator) {
        if (hasAlias) {
            return StringUtils.format("max(\"%s\") AS \"%s\"", aggregator.getField(), aggregator.getName());
        } else {
            return StringUtils.format("max(\"%s\")", aggregator.getField());
        }
    }

    @Override
    public String visit(QueryStageAggregators.MinAggregator aggregator) {
        if (hasAlias) {
            return StringUtils.format("min(\"%s\") AS \"%s\"", aggregator.getField(), aggregator.getName());
        } else {
            return StringUtils.format("min(\"%s\")", aggregator.getField());
        }
    }

    /**
     * Clone a generator with alias disabled
     */
    public QueryStageAggregatorSQLGenerator noAlias() {
        return new QueryStageAggregatorSQLGenerator(this.formatter, this.length, this.step, false);
    }
}
