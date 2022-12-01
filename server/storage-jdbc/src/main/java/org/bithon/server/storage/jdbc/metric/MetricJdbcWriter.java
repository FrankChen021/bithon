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
import org.bithon.component.commons.utils.CollectionUtils;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.storage.datasource.input.IInputRow;
import org.bithon.server.storage.metrics.IMetricWriter;
import org.jooq.BatchBindStep;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.springframework.dao.DuplicateKeyException;

import java.sql.Timestamp;
import java.util.List;

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

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public void write(List<IInputRow> inputRowList) {
        if (CollectionUtils.isEmpty(inputRowList)) {
            return;
        }

        int fieldCount = 1 + table.getDimensions().size() + table.getMetrics().size();

        BatchBindStep step = dsl.batch(dsl.insertInto(table).values(new Object[fieldCount]));


        for (IInputRow inputRow : inputRowList) {
            Object[] values = new Object[fieldCount];

            int index = 0;
            values[index++] = new Timestamp(inputRow.getColAsLong("timestamp"));

            // dimensions
            for (Field dimension : table.getDimensions()) {
                Object value = inputRow.getCol(dimension.getName(), "");
                values[index++] = getOrTruncateDimension(dimension, value);
            }

            // metrics
            for (Field metric : table.getMetrics()) {
                values[index++] = inputRow.getCol(metric.getName(), 0);
            }

            step.bind(values);
        }

        try {
            step.execute();
        } catch (DuplicateKeyException e) {
            log.error("Duplicate Key", e);
        } catch (Exception e) {
            log.error(StringUtils.format("Failed to insert records into [%s].", this.table.getName()),
                      e);
        }
    }

    @Override
    public void close() {
    }

    private String getOrTruncateDimension(Field<?> dimensionField, Object value) {
        if (dimensionField.getDataType().hasLength()) {
            int len = dimensionField.getDataType().length();

            String v = value.toString();
            if (v.length() > len) {
                return v.substring(0, len - 3) + "...";
            }
        }
        return value.toString();
    }
}
