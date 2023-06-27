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
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.storage.datasource.aggregator.NumberAggregator;
import org.bithon.server.storage.datasource.column.aggregatable.count.AggregateCountColumnSpec;
import org.bithon.server.storage.datasource.column.aggregatable.IAggregatableColumnSpec;
import org.bithon.server.storage.datasource.column.aggregatable.gauge.AggregateDoubleLastColumnSpec;
import org.bithon.server.storage.datasource.column.aggregatable.gauge.AggregateLongLastColumnSpec;
import org.bithon.server.storage.datasource.column.aggregatable.max.AggregateLongMaxColumnSpec;
import org.bithon.server.storage.datasource.column.aggregatable.min.AggregateLongMinColumnSpec;
import org.bithon.server.storage.datasource.column.aggregatable.sum.AggregateDoubleSumColumnSpec;
import org.bithon.server.storage.datasource.column.aggregatable.sum.AggregateLongSumColumnSpec;
import org.bithon.server.storage.datasource.query.ast.ResultColumn;
import org.bithon.server.storage.datasource.query.ast.SimpleAggregateExpression;
import org.bithon.server.storage.datasource.typing.IDataType;

/**
 * @author Frank Chen
 * @date 29/10/22 10:47 pm
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", defaultImpl = StringColumnSpec.class)
@JsonSubTypes(value = {
    @JsonSubTypes.Type(name = "long", value = LongColumnSpec.class),
    @JsonSubTypes.Type(name = "string", value = StringColumnSpec.class),
    @JsonSubTypes.Type(name = IColumnSpec.LONG_SUM, value = AggregateLongSumColumnSpec.class),
    @JsonSubTypes.Type(name = IColumnSpec.LONG_LAST, value = AggregateLongLastColumnSpec.class),
    @JsonSubTypes.Type(name = IColumnSpec.LONG_MIN, value = AggregateLongMinColumnSpec.class),
    @JsonSubTypes.Type(name = IColumnSpec.LONG_MAX, value = AggregateLongMaxColumnSpec.class),
    @JsonSubTypes.Type(name = IColumnSpec.DOUBLE_SUM, value = AggregateDoubleSumColumnSpec.class),
    @JsonSubTypes.Type(name = IColumnSpec.DOUBLE_LAST, value = AggregateDoubleLastColumnSpec.class),
    @JsonSubTypes.Type(name = IColumnSpec.POST, value = ExpressionColumnSpec.class),
    @JsonSubTypes.Type(name = IColumnSpec.COUNT, value = AggregateCountColumnSpec.class),
})
public interface IColumnSpec {
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
    String COUNT = "count";
    String LONG_MIN = "longMin";
    String LONG_MAX = "longMax";

    /**
     * temporarily for compatibility only
     */
    default boolean isVisible() {
        return true;
    }

    /**
     * the name in the storage.
     * can NOT be null
     */
    String getName();

    String getAlias();

    String getDisplayText();

    default NumberAggregator createAggregator() {
        throw new UnsupportedOperationException(StringUtils.format("createAggregator is not supported on type of " + this.getClass().getSimpleName()));
    }

    @JsonIgnore
    default SimpleAggregateExpression getAggregateExpression() {
        throw new UnsupportedOperationException(StringUtils.format("getAggregateExpression is not supported on type of " + this.getClass().getSimpleName()));
    }

    @JsonIgnore
    IDataType getDataType();

    @JsonIgnore
    ResultColumn getResultColumn();
}
