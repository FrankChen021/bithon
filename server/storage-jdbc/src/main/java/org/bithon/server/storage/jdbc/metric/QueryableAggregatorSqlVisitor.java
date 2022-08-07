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
import org.bithon.server.storage.datasource.api.CardinalityAggregator;
import org.bithon.server.storage.datasource.api.CountAggregator;
import org.bithon.server.storage.datasource.api.GroupConcatAggregator;
import org.bithon.server.storage.datasource.api.IQueryableAggregatorVisitor;
import org.bithon.server.storage.datasource.api.SimpleAggregator;

/**
 * @author frank.chen021@outlook.com
 * @date 1/11/21 3:11 pm
 */
public class QueryableAggregatorSqlVisitor implements IQueryableAggregatorVisitor<String> {

    private final ISqlExpressionFormatter expressionFormatter;

    public QueryableAggregatorSqlVisitor(ISqlExpressionFormatter expressionFormatter) {
        this.expressionFormatter = expressionFormatter;
    }

    @Override
    public String visit(CardinalityAggregator aggregator) {
        return StringUtils.format("count(DISTINCT \"%s\") AS \"%s\"", aggregator.getDimension(), aggregator.getName());
    }

    @Override
    public String visit(GroupConcatAggregator aggregator) {
        return expressionFormatter.stringAggregator(aggregator.getField(), aggregator.getName());
    }

    @Override
    public String visit(SimpleAggregator aggregator) {
        return StringUtils.format("%s(\"%s\") AS \"%s\"", aggregator.getAggregator(), aggregator.getField(), aggregator.getName());
    }

    @Override
    public String visit(CountAggregator aggregator) {
        return StringUtils.format("count(1) AS \"%s\"", aggregator.getName());
    }
}
