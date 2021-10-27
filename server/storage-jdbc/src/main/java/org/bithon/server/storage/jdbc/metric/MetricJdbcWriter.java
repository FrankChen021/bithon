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
import lombok.extern.slf4j.Slf4j;
import org.bithon.server.metric.DataSourceSchema;
import org.bithon.server.metric.aggregator.spec.IMetricSpec;
import org.bithon.server.metric.aggregator.spec.PostAggregatorMetricSpec;
import org.bithon.server.metric.dimension.IDimensionSpec;
import org.bithon.server.metric.input.InputRow;
import org.bithon.server.metric.input.MetricSet;
import org.bithon.server.metric.storage.IMetricWriter;
import org.bithon.server.metric.typing.DoubleValueType;
import org.bithon.server.metric.typing.IValueType;
import org.bithon.server.metric.typing.LongValueType;
import org.bithon.server.metric.typing.StringValueType;
import org.jooq.CreateTableIndexStep;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Index;
import org.jooq.InsertSetMoreStep;
import org.jooq.Query;
import org.jooq.SortField;
import org.jooq.impl.DSL;
import org.jooq.impl.Internal;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/31 1:39 下午
 */
@Slf4j
class MetricJdbcWriter implements IMetricWriter {
    private static final int BATCH_SIZE = 20;
    private final DSLContext dsl;
    private final MetricTable table;

    public MetricJdbcWriter(DSLContext dsl, DataSourceSchema schema) {
        this.dsl = dsl;
        this.table = new MetricTable(schema);

        if ("CLICKHOUSE".equals(dsl.dialect().name())) {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("CREATE TABLE IF NOT EXISTS `%s` (\n", table.getName()));
            for (Field f : table.fields()) {

                if (f.getDataType().hasPrecision()) {
                    sb.append(String.format("`%s` %s(%d, %d) ,\n",
                                            f.getName(),
                                            f.getDataType().getTypeName(),
                                            f.getDataType().precision(),
                                            f.getDataType().scale()));
                } else {
                    sb.append(String.format("`%s` %s ,\n", f.getName(), f.getDataType().getTypeName()));
                }
            }
            sb.delete(sb.length() - 2, sb.length());
            sb.append(") ENGINE=MergeTree ORDER BY(");
            Index idx = table.getIndex(schema.isEnforceDuplicationCheck());
            for (SortField f : idx.getFields()) {
                sb.append(String.format("`%s`,", f.getName()));
            }
            sb.delete(sb.length() - 1, sb.length());
            sb.append(");");
            dsl.execute(sb.toString());
        } else {
            CreateTableIndexStep s = dsl.createTableIfNotExists(table)
                                        .columns(table.fields())
                                        .index(table.getIndex(schema.isEnforceDuplicationCheck()));
            s.execute();
        }
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
            } catch (DuplicateKeyException e) {
                // TODO: this is a bug???
                log.error("Duplicate Key:{}", inputRow);
            }
        }
    }

    @Override
    public void write(List<InputRow> inputRowList) {

        int index = 0;
        int thisBatch;
        List<Query> queries = new ArrayList<>(BATCH_SIZE);
        for (int leftSize = inputRowList.size(); leftSize > 0; leftSize -= thisBatch) {
            thisBatch = Math.min(BATCH_SIZE, leftSize);

            queries.clear();
            for (int i = 0; i < thisBatch; i++, index++) {
                queries.add(toInsertSql(inputRowList.get(index)));
            }

            try {
                dsl.batch(queries.toArray(new Query[0])).execute();
            } catch (DuplicateKeyException e) {
                log.error("Duplicate Key:{}", queries);
            } catch (Exception e) {
                log.error("Failed to insert records into [{}]. Error message: {}. SQL:{}",
                          this.table.getName(),
                          e.getMessage(),
                          queries.stream().map(Query::getSQL).collect(Collectors.joining("\n")));
            }
        }
    }

    @Override
    public void write(Collection<MetricSet> metricSetList) {
        int index = 0;
        int thisBatch;
        List<Query> queries = new ArrayList<>(BATCH_SIZE);
        for (int leftSize = metricSetList.size(); leftSize > 0; leftSize -= thisBatch) {
            thisBatch = Math.min(BATCH_SIZE, leftSize);

            queries.clear();
            for (int i = 0; i < thisBatch; i++, index++) {
                queries.add(toInsertSql(((List<MetricSet>) metricSetList).get(index)));
            }

            try {
                dsl.batch(queries.toArray(new Query[0])).execute();
            } catch (DuplicateKeyException e) {
                log.error("Duplicate Key:{}", queries);
            } catch (Exception e) {
                log.error("Failed to insert records into [{}]. Error message: {}. SQL:{}",
                          this.table.getName(),
                          e.getMessage(),
                          queries.stream().map(Query::getSQL).collect(Collectors.joining("\n")));
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void deleteBefore(long timestamp) {
        if ("CLICKHOUSE".equals(dsl.dialect().name())) {
            return;
        }
        dsl.deleteFrom(table).where(table.timestampField.lt(new Timestamp(timestamp))).execute();
    }

    @SuppressWarnings("unchecked")
    private InsertSetMoreStep<?> toInsertSql(InputRow inputRow) {
        InsertSetMoreStep<?> step = dsl.insertInto(table)
                                       .set(table.timestampField, new Timestamp(inputRow.getColAsLong("timestamp")));

        for (Field dimension : table.dimensions) {
            Object value = inputRow.getCol(dimension.getName(), "");
            step.set(dimension, value);
        }
        for (Field metric : table.metrics) {
            Object value = inputRow.getCol(metric.getName(), 0);
            step.set(metric, value);
        }

        return step;
    }

    @SuppressWarnings("unchecked")
    private InsertSetMoreStep toInsertSql(MetricSet metricSet) {
        InsertSetMoreStep<?> step = dsl.insertInto(table)
                                       .set(table.timestampField,
                                            new Timestamp(metricSet.getTimestamp()));

        for (Field dimension : table.dimensions) {
            Object value = metricSet.getDimension(dimension.getName(), "");
            step.set(dimension, value);
        }
        for (Field metric : table.metrics) {
            Object value = metricSet.getMetric(metric.getName(), 0);
            step.set(metric, value);
        }

        return step;
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
}
