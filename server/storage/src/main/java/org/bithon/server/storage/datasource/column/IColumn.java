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

package org.bithon.server.storage.datasource.column;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.bithon.component.commons.expression.FunctionExpression;
import org.bithon.component.commons.expression.IDataType;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.IdentifierExpression;
import org.bithon.component.commons.expression.function.IFunction;
import org.bithon.component.commons.expression.validation.IIdentifier;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.storage.datasource.aggregator.NumberAggregator;
import org.bithon.server.storage.datasource.column.aggregatable.last.AggregateDoubleLastColumn;
import org.bithon.server.storage.datasource.column.aggregatable.last.AggregateLongLastColumn;
import org.bithon.server.storage.datasource.column.aggregatable.max.AggregateLongMaxColumn;
import org.bithon.server.storage.datasource.column.aggregatable.min.AggregateLongMinColumn;
import org.bithon.server.storage.datasource.column.aggregatable.sum.AggregateDoubleSumColumn;
import org.bithon.server.storage.datasource.column.aggregatable.sum.AggregateLongSumColumn;
import org.bithon.server.storage.datasource.query.ast.Selector;

/**
 * @author Frank Chen
 * @date 29/10/22 10:47 pm
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", defaultImpl = StringColumn.class)
@JsonSubTypes(value = {
    @JsonSubTypes.Type(name = "long", value = LongColumn.class),
    @JsonSubTypes.Type(name = "string", value = StringColumn.class),
    @JsonSubTypes.Type(name = "datetime", value = DateTimeColumn.class),
    @JsonSubTypes.Type(name = "object", value = ObjectColumn.class),
    @JsonSubTypes.Type(name = "boolean", value = BooleanColumn.class),
    @JsonSubTypes.Type(name = IColumn.LONG_SUM, value = AggregateLongSumColumn.class),
    @JsonSubTypes.Type(name = IColumn.LONG_LAST, value = AggregateLongLastColumn.class),
    @JsonSubTypes.Type(name = IColumn.LONG_MIN, value = AggregateLongMinColumn.class),
    @JsonSubTypes.Type(name = IColumn.LONG_MAX, value = AggregateLongMaxColumn.class),
    @JsonSubTypes.Type(name = IColumn.DOUBLE_SUM, value = AggregateDoubleSumColumn.class),
    @JsonSubTypes.Type(name = IColumn.DOUBLE_LAST, value = AggregateDoubleLastColumn.class),
    @JsonSubTypes.Type(name = IColumn.POST, value = ExpressionColumn.class)
})
public interface IColumn extends IIdentifier {
    /**
     * for Gauge
     */
    String LONG_LAST = "longLast";
    String DOUBLE_LAST = "doubleLast";

    /**
     * for Counter
     */
    String LONG_SUM = "longSum";
    String DOUBLE_SUM = "doubleSum";
    String POST = "post";
    String LONG_MIN = "longMin";
    String LONG_MAX = "longMax";

    /**
     * the name in the storage.
     * can NOT be null
     */
    String getName();

    String getAlias();

    default NumberAggregator createAggregator() {
        throw new UnsupportedOperationException(StringUtils.format("createAggregator is not supported on type of " + this.getClass().getSimpleName()));
    }

    /**
     * Different column type may have different aggregator names.
     * For example, In ClickHouse, sumMerge is used for AggregatingMergeTree,
     * so this interface provides an opportunity for subclasses to override the default aggregator name
     */
    default IExpression createAggregateFunctionExpression(IFunction function) {
        return new FunctionExpression(function, new IdentifierExpression(getName()));
    }

    IDataType getDataType();

    @JsonIgnore
    Selector toSelector();
}
