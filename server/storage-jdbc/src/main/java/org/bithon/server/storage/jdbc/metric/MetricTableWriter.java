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
import org.bithon.component.commons.utils.RetryUtils;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.storage.datasource.input.IInputRow;
import org.bithon.server.storage.jdbc.common.IOnceTableWriter;
import org.jooq.Field;
import org.springframework.dao.DuplicateKeyException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.List;
import java.util.function.Predicate;

/**
 * @author Frank Chen
 * @date 16/1/24 8:15 pm
 */
@Slf4j
class MetricTableWriter implements IOnceTableWriter {
    private final String insertStatement;
    private final MetricTable table;
    private final List<IInputRow> inputRowList;
    private final boolean truncateDimension;
    private final Predicate<Exception> isRetryableException;

    public MetricTableWriter(String insertStatement,
                             MetricTable table,
                             List<IInputRow> inputRowList,
                             boolean truncateDimension,
                             Predicate<Exception> isRetryableException) {
        this.insertStatement = insertStatement;
        this.table = table;
        this.inputRowList = inputRowList;
        this.truncateDimension = truncateDimension;
        this.isRetryableException = isRetryableException;
    }

    @Override
    public String getTable() {
        return table.getName();
    }

    @Override
    public int getInsertSize() {
        return inputRowList.size();
    }

    @Override
    public void run(Connection connection) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(this.insertStatement)) {
            for (IInputRow inputRow : inputRowList) {

                int index = 1;
                statement.setObject(index++, new Timestamp(inputRow.getColAsLong("timestamp")).toLocalDateTime());

                // dimensions
                for (Field<?> dimension : table.getDimensions()) {
                    // the value might be type of integer, so Object should be used
                    Object value = inputRow.getCol(dimension.getName(), "");
                    statement.setObject(index++, truncateDimension ? getOrTruncateDimension(dimension, value.toString()) : value.toString());
                }

                // metrics
                for (Field<?> metric : table.getMetrics()) {
                    statement.setObject(index++, inputRow.getCol(metric.getName(), 0));
                }

                statement.addBatch();
            }

            try {
                RetryUtils.retry(statement::executeBatch,
                                 this::isExceptionRetryable,
                                 3,
                                 Duration.ofMillis(100));
            } catch (DuplicateKeyException e) {
                log.error("Duplicate Key", e);
            } catch (Exception e) {
                log.error(StringUtils.format("Failed to insert records into [%s].", this.table.getName()),
                          e);
            }
        }
    }

    protected boolean isExceptionRetryable(Exception e) {
        return this.isRetryableException != null && this.isRetryableException.test(e);
    }

    private String getOrTruncateDimension(Field<?> dimensionField, String value) {
        if (dimensionField.getDataType().hasLength()) {
            int len = dimensionField.getDataType().length();

            if (value.length() > len) {
                return value.substring(0, len - 3) + "...";
            }
        }
        return value;
    }
}
