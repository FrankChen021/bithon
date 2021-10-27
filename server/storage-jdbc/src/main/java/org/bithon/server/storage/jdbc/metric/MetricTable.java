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

import lombok.Getter;
import org.bithon.server.metric.DataSourceSchema;
import org.bithon.server.metric.aggregator.spec.IMetricSpec;
import org.bithon.server.metric.aggregator.spec.PostAggregatorMetricSpec;
import org.bithon.server.metric.dimension.IDimensionSpec;
import org.bithon.server.metric.typing.DoubleValueType;
import org.bithon.server.metric.typing.IValueType;
import org.bithon.server.metric.typing.LongValueType;
import org.bithon.server.metric.typing.StringValueType;
import org.jooq.Field;
import org.jooq.Index;
import org.jooq.impl.DSL;
import org.jooq.impl.Internal;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Frank Chen
 * @date 27/10/21 10:07 pm
 */
@SuppressWarnings("rawtypes")
public class MetricTable extends TableImpl {
    @Getter
    private final List<Field> dimensions = new ArrayList<>();
    @Getter
    private final List<Field> metrics = new ArrayList<>();
    Field<Timestamp> timestampField;

    public MetricTable(DataSourceSchema schema) {
        super(DSL.name("bithon_" + schema.getName().replace("-", "_")));

        //noinspection unchecked
        timestampField = this.createField(DSL.name("timestamp"), SQLDataType.TIMESTAMP(3));

        for (IDimensionSpec dimension : schema.getDimensionsSpec()) {
            dimensions.add(createField(dimension.getName(), dimension.getValueType()));
        }

        for (IMetricSpec metric : schema.getMetricsSpec()) {
            if (metric instanceof PostAggregatorMetricSpec) {
                continue;
            }
            metrics.add(createField(metric.getName(), metric.getValueType()));
        }
    }

    public Index getIndex(boolean unique) {
        List<Field> indexesFields = new ArrayList<>();
        indexesFields.add(timestampField);
        indexesFields.addAll(dimensions);
        return Internal.createIndex("idx_" + this.getName() + "_dimensions",
                                    this,
                                    indexesFields.toArray(new Field[0]),
                                    unique);

    }

    private Field createField(String name, IValueType valueType) {
        if (valueType.equals(DoubleValueType.INSTANCE)) {
            return this.createField(DSL.name(name),
                                    SQLDataType.DECIMAL(18, 2).nullable(false).defaultValue(BigDecimal.valueOf(0)));
        } else if (valueType.equals(LongValueType.INSTANCE)) {
            return this.createField(DSL.name(name), SQLDataType.BIGINT.nullable(false).defaultValue(0L));
        } else if (valueType.equals(StringValueType.INSTANCE)) {
            return this.createField(DSL.name(name), SQLDataType.VARCHAR(128).nullable(false).defaultValue(""));
        } else {
            throw new RuntimeException("unknown type:" + valueType);
        }
    }
}
