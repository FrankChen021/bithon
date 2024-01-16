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

package org.bithon.server.storage.jdbc.tracing.writer;

import org.bithon.component.commons.utils.RetryUtils;
import org.bithon.server.storage.jdbc.common.IOnceTableWriter;
import org.bithon.server.storage.jdbc.common.jooq.Tables;
import org.bithon.server.storage.tracing.mapping.TraceIdMapping;
import org.jooq.DSLContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.Collection;
import java.util.function.Predicate;

/**
 * @author Frank Chen
 * @date 16/1/24 8:01 pm
 */
class MappingTableWriter implements IOnceTableWriter {
    private final String insertStatement;
    private final Collection<TraceIdMapping> mappings;
    private final Predicate<Exception> isExceptionRetryable;

    public MappingTableWriter(DSLContext dslContext,
                              Collection<TraceIdMapping> mappings,
                              Predicate<Exception> isRetryableException) {
        insertStatement = dslContext.render(dslContext.insertInto(Tables.BITHON_TRACE_MAPPING,
                                                                  Tables.BITHON_TRACE_MAPPING.TRACE_ID,
                                                                  Tables.BITHON_TRACE_MAPPING.USER_TX_ID,
                                                                  Tables.BITHON_TRACE_SPAN.TIMESTAMP)
                                                      .values((String) null, null, null));
        this.mappings = mappings;
        this.isExceptionRetryable = isRetryableException;
    }

    @Override
    public void run(Connection connection) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(insertStatement)) {
            for (TraceIdMapping mapping : mappings) {
                statement.setObject(1, mapping.getTraceId());
                statement.setObject(2, mapping.getUserId());
                statement.setObject(3, new Timestamp(mapping.getTimestamp()).toLocalDateTime());
                statement.addBatch();
            }

            RetryUtils.retry(statement::executeBatch,
                             this::isExceptionRetryable,
                             3,
                             Duration.ofMillis(100));
        }
    }

    protected boolean isExceptionRetryable(Exception e) {
        return isExceptionRetryable != null && this.isExceptionRetryable.test(e);
    }

    @Override
    public String getTable() {
        return Tables.BITHON_TRACE_MAPPING.getName();
    }

    @Override
    public int getInsertSize() {
        return this.mappings.size();
    }
}
