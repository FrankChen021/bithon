package com.sbss.bithon.server.metric.storage.jdbc;

import com.sbss.bithon.server.metric.DataSourceSchema;
import com.sbss.bithon.server.metric.aggregator.IMetricSpec;
import com.sbss.bithon.server.metric.aggregator.PostAggregatorMetricSpec;
import com.sbss.bithon.server.metric.dimension.IDimensionSpec;
import com.sbss.bithon.server.metric.input.InputRow;
import com.sbss.bithon.server.metric.storage.IMetricWriter;
import com.sbss.bithon.server.metric.typing.DoubleValueType;
import com.sbss.bithon.server.metric.typing.IValueType;
import com.sbss.bithon.server.metric.typing.LongValueType;
import com.sbss.bithon.server.metric.typing.StringValueType;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jooq.CreateTableIndexStep;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Index;
import org.jooq.InsertSetMoreStep;
import org.jooq.impl.DSL;
import org.jooq.impl.Internal;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/31 1:39 下午
 */
@Slf4j
class MetricJdbcWriter implements IMetricWriter {
    private final DSLContext dsl;
    private final MetricTable table;

    public MetricJdbcWriter(DSLContext dsl, DataSourceSchema schema) {
        this.dsl = dsl;
        this.table = new MetricTable(schema);

        CreateTableIndexStep s = dsl.createTableIfNotExists(table).columns(table.fields()).index(table.getIndex());
        String sql = s.getSQL();
        s.execute();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public void write(InputRow inputRow) {
        try (InsertSetMoreStep step = dsl.insertInto(table)
                                         .set(table.timestampField,
                                              new Timestamp(inputRow.getColAsLong("timestamp")))) {

            for (Field dimension : table.dimensions) {
                Object value = inputRow.getCol(dimension.getName(), "");
                step.set(dimension, value);
            }
            for (Field metric : table.metrics) {
                Object value = inputRow.getCol(metric.getName(), 0);
                step.set(metric, value);
            }

            try {
                step.execute();
            } catch(DuplicateKeyException e) {
                // TODO: this is a bug???
                log.error("Duplicate Key:{}", inputRow);
            }
        }
    }

    @Override
    public void close() {
    }

    @SuppressWarnings("rawtypes")
    static class MetricTable extends TableImpl {
        @Getter
        private final List<Field> dimensions = new ArrayList<>();
        @Getter
        private final List<Field> metrics = new ArrayList<>();
        Field timestampField;

        public MetricTable(DataSourceSchema schema) {
            super(DSL.name("bithon_" + schema.getName().replace("-", "_")));

            timestampField = this.createField(DSL.name("timestamp"), SQLDataType.TIMESTAMP);

            for (IDimensionSpec dimension : schema.getDimensionsSpec()) {
                dimensions.add(createField(dimension.getName(), dimension.getValueType()));
            }

            for (IMetricSpec metric : schema.getMetricsSpec()) {
                if ( metric instanceof PostAggregatorMetricSpec ) {
                    continue;
                }
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
}
