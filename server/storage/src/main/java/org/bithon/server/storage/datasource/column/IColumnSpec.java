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
import org.bithon.server.storage.datasource.column.metric.CountMetricSpec;
import org.bithon.server.storage.datasource.column.metric.IMetricSpec;
import org.bithon.server.storage.datasource.column.metric.PostAggregatorMetricSpec;
import org.bithon.server.storage.datasource.column.metric.gauge.DoubleGaugeMetricSpec;
import org.bithon.server.storage.datasource.column.metric.gauge.LongGaugeMetricSpec;
import org.bithon.server.storage.datasource.column.metric.max.LongMaxMetricSpec;
import org.bithon.server.storage.datasource.column.metric.min.LongMinMetricSpec;
import org.bithon.server.storage.datasource.column.metric.sum.DoubleSumMetricSpec;
import org.bithon.server.storage.datasource.column.metric.sum.LongSumMetricSpec;
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
    @JsonSubTypes.Type(name = IMetricSpec.LONG_SUM, value = LongSumMetricSpec.class),
    @JsonSubTypes.Type(name = IMetricSpec.LONG_LAST, value = LongGaugeMetricSpec.class),
    @JsonSubTypes.Type(name = IMetricSpec.LONG_MIN, value = LongMinMetricSpec.class),
    @JsonSubTypes.Type(name = IMetricSpec.LONG_MAX, value = LongMaxMetricSpec.class),
    @JsonSubTypes.Type(name = IMetricSpec.DOUBLE_SUM, value = DoubleSumMetricSpec.class),
    @JsonSubTypes.Type(name = IMetricSpec.DOUBLE_LAST, value = DoubleGaugeMetricSpec.class),
    @JsonSubTypes.Type(name = IMetricSpec.POST, value = PostAggregatorMetricSpec.class),
    @JsonSubTypes.Type(name = IMetricSpec.COUNT, value = CountMetricSpec.class),
})
public interface IColumnSpec {
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
