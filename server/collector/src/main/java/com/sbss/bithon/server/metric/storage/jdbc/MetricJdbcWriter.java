package com.sbss.bithon.server.metric.storage.jdbc;

import com.sbss.bithon.server.metric.DataSourceSchema;
import com.sbss.bithon.server.metric.dimension.IDimensionSpec;
import com.sbss.bithon.server.metric.input.InputRow;
import com.sbss.bithon.server.metric.aggregator.IMetricSpec;
import com.sbss.bithon.server.metric.storage.IMetricWriter;
import com.sbss.bithon.server.metric.typing.DoubleValueType;
import com.sbss.bithon.server.metric.typing.IValueType;
import com.sbss.bithon.server.metric.typing.LongValueType;
import com.sbss.bithon.server.metric.typing.StringValueType;
import lombok.Getter;
import org.jooq.*;
import org.jooq.impl.DSL;
import org.jooq.impl.Internal;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/31 1:39 下午
 */
class MetricJdbcWriter implements IMetricWriter {
    private final DSLContext dsl;
    private final MetricTable table;

    public MetricJdbcWriter(DSLContext dsl, DataSourceSchema schema) {
        this.dsl = dsl;
        this.table = new MetricTable(schema);

        CreateTableIndexStep s  = dsl.createTableIfNotExists(table).columns(table.fields()).index(table.getIndex());
        String sql = s.getSQL();
        s.execute();
    }

    @SuppressWarnings("rawtypes")
    static class MetricTable extends TableImpl {
        Field timestampField;

        @Getter
        private final List<Field> dimensions = new ArrayList<>();

        @Getter
        private final List<Field> metrics = new ArrayList<>();

        public MetricTable(DataSourceSchema schema) {
            super(DSL.name("bithon_" + schema.getName().replace("-", "_")));

            timestampField = this.createField(DSL.name("timestamp"), SQLDataType.TIMESTAMP);

            for (IDimensionSpec dimension : schema.getDimensionsSpec()) {
                dimensions.add(createField(dimension.getName(), dimension.getValueType()));
            }

            for (IMetricSpec metric : schema.getMetricsSpec()) {
                metrics.add(createField(metric.getName(), metric.getValueType()));
            }
        }

        public Index getIndex() {
            List<Field> indexesFields = new ArrayList<>();
            indexesFields.add(timestampField);
            indexesFields.addAll(dimensions);
            return Internal.createIndex("idx_" + this.getName() + "_dimensions",
                                        this,
                                        indexesFields.toArray(new Field[0]),
                                        true);

        }

        private Field createField(String name, IValueType valueType) {
            if (valueType.equals(DoubleValueType.INSTANCE)) {
                return this.createField(DSL.name(name), SQLDataType.DECIMAL(18, 2).nullable(false).defaultValue(BigDecimal.valueOf(0)));
            } else if (valueType.equals(LongValueType.INSTANCE)) {
                return this.createField(DSL.name(name), SQLDataType.BIGINT.nullable(false).defaultValue(0L));
            } else if (valueType.equals(StringValueType.INSTANCE)) {
                return this.createField(DSL.name(name), SQLDataType.VARCHAR(128).nullable(false).defaultValue(""));
            } else {
                throw new RuntimeException("unknown type:" + valueType);
            }
        }
    }


    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public void write(InputRow inputRow) {
        try (InsertSetMoreStep step = dsl.insertInto(table)
            .set(table.timestampField,
                 new Timestamp(inputRow.getColumnValueAsLong("timestamp")))) {

            for (Field dimension : table.dimensions) {
                Object value = inputRow.getColumnValue(dimension.getName(), "");
                step.set(dimension, value);
            }
            for (Field metric : table.metrics) {
                Object value = inputRow.getColumnValue(metric.getName(), 0);
                step.set(metric, value);
            }

            step.execute();
        }
    }

    @Override
    public void close() {
    }
}
