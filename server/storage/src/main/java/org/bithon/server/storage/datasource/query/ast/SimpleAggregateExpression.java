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
    @JsonSubTypes.Type(name = SimpleAggregateExpressions.CardinalityAggregateExpression.TYPE, value = SimpleAggregateExpressions.CardinalityAggregateExpression.class),
    @JsonSubTypes.Type(name = SimpleAggregateExpressions.SumAggregateExpression.TYPE, value = SimpleAggregateExpressions.SumAggregateExpression.class),
    @JsonSubTypes.Type(name = SimpleAggregateExpressions.CountAggregateExpression.TYPE, value = SimpleAggregateExpressions.CountAggregateExpression.class),
    @JsonSubTypes.Type(name = SimpleAggregateExpressions.AvgAggregateExpression.TYPE, value = SimpleAggregateExpressions.AvgAggregateExpression.class),
    @JsonSubTypes.Type(name = SimpleAggregateExpressions.MinAggregateExpression.TYPE, value = SimpleAggregateExpressions.MinAggregateExpression.class),
    @JsonSubTypes.Type(name = SimpleAggregateExpressions.MaxAggregateExpression.TYPE, value = SimpleAggregateExpressions.MaxAggregateExpression.class),
    @JsonSubTypes.Type(name = SimpleAggregateExpressions.FirstAggregateExpression.TYPE, value = SimpleAggregateExpressions.FirstAggregateExpression.class),
    @JsonSubTypes.Type(name = SimpleAggregateExpressions.LastAggregateExpression.TYPE, value = SimpleAggregateExpressions.LastAggregateExpression.class),
    @JsonSubTypes.Type(name = SimpleAggregateExpressions.RateAggregateExpression.TYPE, value = SimpleAggregateExpressions.RateAggregateExpression.class),
    @JsonSubTypes.Type(name = SimpleAggregateExpressions.GroupConcatAggregateExpression.TYPE, value = SimpleAggregateExpressions.GroupConcatAggregateExpression.class),
})
public abstract class SimpleAggregateExpression extends Function {
    public SimpleAggregateExpression(String fnName, String field) {
        super(fnName);
        getArguments().add(new Column(field));
    }

    /**
     * get the column that aggregation is performed on
     */
    public String getTargetColumn() {
        return ((Column) this.getArguments().get(0)).getName();
    }

    public abstract <T> T accept(ISimpleAggregateFunctionVisitor<T> visitor);

    @Override
    public String toString() {
        return StringUtils.format("%s(%s)", this.getFnName(), getTargetColumn());
    }
}
