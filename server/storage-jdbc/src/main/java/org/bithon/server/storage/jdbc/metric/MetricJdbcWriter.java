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

import lombok.extern.slf4j.Slf4j;
import org.bithon.server.metric.input.InputRow;
import org.bithon.server.metric.input.Measurement;
import org.bithon.server.metric.storage.IMetricWriter;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.InsertSetMoreStep;
import org.jooq.Query;
import org.springframework.dao.DuplicateKeyException;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/31 1:39 下午
 */
@Slf4j
class MetricJdbcWriter implements IMetricWriter {
    private final DSLContext dsl;
    private final MetricTable table;

    public MetricJdbcWriter(DSLContext dsl, MetricTable table) {
        this.dsl = dsl;
        this.table = table;
    }

    @Override
    public void write(List<InputRow> inputRowList) {
        writeRows(inputRowList.stream().map(this::toInsertSql).collect(Collectors.toList()));
    }

    @Override
    public void write(Collection<Measurement> measurementList) {
        writeRows(measurementList.stream().map(this::toInsertSql).collect(Collectors.toList()));
    }

    private void writeRows(List<Query> queries) {
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

    @SuppressWarnings("unchecked")
    private InsertSetMoreStep<?> toInsertSql(InputRow inputRow) {
        InsertSetMoreStep<?> step = dsl.insertInto(table)
                                       .set(table.timestampField, new Timestamp(inputRow.getColAsLong("timestamp")));

        //noinspection rawtypes
        for (Field dimension : table.getDimensions()) {
            Object value = inputRow.getCol(dimension.getName(), "");
            step.set(dimension, value);
        }
        //noinspection rawtypes
        for (Field metric : table.getMetrics()) {
            Object value = inputRow.getCol(metric.getName(), 0);
            step.set(metric, value);
        }

        return step;
    }

    @SuppressWarnings("unchecked")
    private InsertSetMoreStep<?> toInsertSql(Measurement measurement) {
        InsertSetMoreStep<?> step = dsl.insertInto(table)
                                       .set(table.timestampField,
                                            new Timestamp(measurement.getTimestamp()));

        //noinspection rawtypes
        for (Field dimension : table.getDimensions()) {
            Object value = measurement.getDimension(dimension.getName(), "");
            step.set(dimension, value);
        }
        //noinspection rawtypes
        for (Field metric : table.getMetrics()) {
            Object value = measurement.getMetric(metric.getName(), 0);
            step.set(metric, value);
        }

        return step;
    }

    @Override
    public void close() {
    }
}
