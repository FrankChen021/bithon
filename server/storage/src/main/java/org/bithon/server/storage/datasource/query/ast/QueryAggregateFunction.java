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

package org.bithon.server.storage.datasource.query.ast;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.bithon.component.commons.utils.StringUtils;


/**
 * Aggregator for built-in types.
 *
 * @author Frank Chen
 * @date 1/11/21 2:36 pm
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(value = {
    @JsonSubTypes.Type(name = QueryAggregateFunctions.CardinalityAggregateExpression.TYPE, value = QueryAggregateFunctions.CardinalityAggregateExpression.class),
    @JsonSubTypes.Type(name = QueryAggregateFunctions.SumAggregateExpression.TYPE, value = QueryAggregateFunctions.SumAggregateExpression.class),
    @JsonSubTypes.Type(name = QueryAggregateFunctions.CountAggregateExpression.TYPE, value = QueryAggregateFunctions.CountAggregateExpression.class),
    @JsonSubTypes.Type(name = QueryAggregateFunctions.AvgAggregateExpression.TYPE, value = QueryAggregateFunctions.AvgAggregateExpression.class),
    @JsonSubTypes.Type(name = QueryAggregateFunctions.MinAggregateExpression.TYPE, value = QueryAggregateFunctions.MinAggregateExpression.class),
    @JsonSubTypes.Type(name = QueryAggregateFunctions.MaxAggregateExpression.TYPE, value = QueryAggregateFunctions.MaxAggregateExpression.class),
    @JsonSubTypes.Type(name = QueryAggregateFunctions.FirstAggregateExpression.TYPE, value = QueryAggregateFunctions.FirstAggregateExpression.class),
    @JsonSubTypes.Type(name = QueryAggregateFunctions.LastAggregateExpression.TYPE, value = QueryAggregateFunctions.LastAggregateExpression.class),
    @JsonSubTypes.Type(name = QueryAggregateFunctions.RateAggregateExpression.TYPE, value = QueryAggregateFunctions.RateAggregateExpression.class),
    @JsonSubTypes.Type(name = QueryAggregateFunctions.GroupConcatAggregateExpression.TYPE, value = QueryAggregateFunctions.GroupConcatAggregateExpression.class),
})
public abstract class QueryAggregateFunction extends Function {
    public QueryAggregateFunction(String fnName, String field) {
        super(fnName);
        getArguments().add(new Column(field));
    }

    /**
     * get the column that aggregation is performed on
     */
    public String getTargetColumn() {
        return ((Column) this.getArguments().get(0)).getName();
    }

    public abstract <T> T accept(IQueryAggregateFunctionVisitor<T> visitor);

    @Override
    public String toString() {
        return StringUtils.format("%s(%s)", this.getFnName(), getTargetColumn());
    }
}
