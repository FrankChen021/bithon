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
import org.bithon.server.storage.jdbc.common.jooq.Tables;
import org.jooq.DSLContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;

/**
 * @author Frank Chen
 * @date 16/1/24 8:02 pm
 */
public class IndexTableWriter implements ITableWriter {
    private final String insertStatement;
    private final Collection<Object[]> tagIndices;

    public IndexTableWriter(DSLContext dslContext, Collection<Object[]> tagIndices) {
        this.insertStatement = dslContext.render(dslContext.insertInto(Tables.BITHON_TRACE_SPAN_TAG_INDEX,
                                                                       Tables.BITHON_TRACE_SPAN_TAG_INDEX.TIMESTAMP,
                                                                       Tables.BITHON_TRACE_SPAN_TAG_INDEX.F1,
                                                                       Tables.BITHON_TRACE_SPAN_TAG_INDEX.F2,
                                                                       Tables.BITHON_TRACE_SPAN_TAG_INDEX.F3,
                                                                       Tables.BITHON_TRACE_SPAN_TAG_INDEX.F4,
                                                                       Tables.BITHON_TRACE_SPAN_TAG_INDEX.F5,
                                                                       Tables.BITHON_TRACE_SPAN_TAG_INDEX.F6,
                                                                       Tables.BITHON_TRACE_SPAN_TAG_INDEX.F7,
                                                                       Tables.BITHON_TRACE_SPAN_TAG_INDEX.F8,
                                                                       Tables.BITHON_TRACE_SPAN_TAG_INDEX.F9,
                                                                       Tables.BITHON_TRACE_SPAN_TAG_INDEX.F10,
                                                                       Tables.BITHON_TRACE_SPAN_TAG_INDEX.F11,
                                                                       Tables.BITHON_TRACE_SPAN_TAG_INDEX.F12,
                                                                       Tables.BITHON_TRACE_SPAN_TAG_INDEX.F13,
                                                                       Tables.BITHON_TRACE_SPAN_TAG_INDEX.F14,
                                                                       Tables.BITHON_TRACE_SPAN_TAG_INDEX.F15,
                                                                       Tables.BITHON_TRACE_SPAN_TAG_INDEX.F16,
                                                                       Tables.BITHON_TRACE_SPAN_TAG_INDEX.TRACEID)
                                                           .values((LocalDateTime) null,
                                                                   null,
                                                                   null,
                                                                   null,
                                                                   null,
                                                                   null,
                                                                   null,
                                                                   null,
                                                                   null,
                                                                   null,
                                                                   null,
                                                                   null,
                                                                   null,
                                                                   null,
                                                                   null,
                                                                   null,
                                                                   null,
                                                                   null));
        this.tagIndices = tagIndices;
    }

    @Override
    public void run(Connection connection) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(insertStatement)) {
            for (Object[] index : tagIndices) {
                for (int i = 0; i < index.length; i++) {
                    statement.setObject(i + 1, index[i]);
                }
                statement.addBatch();
            }

            RetryUtils.retry(statement::executeBatch,
                             this::isExceptionRetryable,
                             3,
                             Duration.ofMillis(100));
        }
    }

    protected boolean isExceptionRetryable(Exception e) {
        return false;
    }

    @Override
    public String getTable() {
        return Tables.BITHON_TRACE_SPAN_TAG_INDEX.getName();
    }

    @Override
    public int getInsertSize() {
        return this.tagIndices.size();
    }
}
