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
import org.bithon.server.storage.datasource.DataSourceSchema;
import org.bithon.server.storage.datasource.dimension.IDimensionSpec;
import org.bithon.server.storage.datasource.spec.IMetricSpec;
import org.bithon.server.storage.datasource.spec.PostAggregatorMetricSpec;
import org.bithon.server.storage.datasource.typing.DoubleValueType;
import org.bithon.server.storage.datasource.typing.IValueType;
import org.bithon.server.storage.datasource.typing.LongValueType;
import org.bithon.server.storage.datasource.typing.StringValueType;
import org.jooq.Field;
import org.jooq.Index;
import org.jooq.impl.DSL;
import org.jooq.impl.Internal;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 27/10/21 10:07 pm
 */
@SuppressWarnings("rawtypes")
public class MetricTable extends TableImpl {
    @Getter
    private final List<Field> dimensions = new ArrayList<>();
    @Getter
    private final List<Field> metrics = new ArrayList<>();
    private final List<Index> indexes;
    private final Field<Timestamp> timestampField;

    public Field<Timestamp> getTimestampField() {
        return timestampField;
    }

    public MetricTable(DataSourceSchema schema) {
        super(DSL.name("bithon_" + schema.getName().replace('-', '_')));

        //noinspection unchecked
        timestampField = this.createField(DSL.name("timestamp"), SQLDataType.TIMESTAMP);

        List<Field> indexesFields = new ArrayList<>();
        indexesFields.add(timestampField);

        for (IDimensionSpec dimension : schema.getDimensionsSpec()) {
            Field dimensionField = createField(dimension.getName(), dimension.getValueType(), dimension.getLength());
            dimensions.add(dimensionField);

            if (dimension.isVisible()) {
                indexesFields.add(dimensionField);
            }
        }

        for (IMetricSpec metric : schema.getMetricsSpec()) {
            if (metric instanceof PostAggregatorMetricSpec) {
                continue;
            }
            metrics.add(createField(metric.getName(), metric.getValueType(), null));
        }

        Index index = Internal.createIndex("idx_" + this.getName() + "_dimensions",
                                           this,
                                           indexesFields.toArray(new Field[0]),
                                           schema.isEnforceDuplicationCheck());
        this.indexes = Collections.singletonList(index);
    }

    @Override
    public List<Index> getIndexes() {
        return indexes;
    }

    private Field createField(String name, IValueType valueType, Integer length) {
        if (valueType.equals(DoubleValueType.INSTANCE)) {
            //noinspection unchecked
            return this.createField(DSL.name(name),
                                    SQLDataType.DECIMAL(18, 2).nullable(false).defaultValue(BigDecimal.valueOf(0)));
        } else if (valueType.equals(LongValueType.INSTANCE)) {
            //noinspection unchecked
            return this.createField(DSL.name(name), SQLDataType.BIGINT.nullable(false).defaultValue(0L));
        } else if (valueType.equals(StringValueType.INSTANCE)) {
            //noinspection unchecked
            return this.createField(DSL.name(name), SQLDataType.VARCHAR(length).nullable(false).defaultValue(""));
        } else {
            throw new RuntimeException("unknown type:" + valueType);
        }
    }
}
