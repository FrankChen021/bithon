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
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.storage.jdbc.common.IOnceTableWriter;
import org.bithon.server.storage.jdbc.common.jooq.Tables;
import org.bithon.server.storage.tracing.TraceSpan;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * @author Frank Chen
 * @date 16/1/24 8:04 pm
 */
public abstract class SpanTableJdbcWriter implements IOnceTableWriter {
    private final String insertStatement;
    private final List<TraceSpan> spans;
    private final String table;
    private final Predicate<Exception> isRetryableException;
    private final boolean isSummaryTable;

    public SpanTableJdbcWriter(String table,
                               String insertStatement,
                               List<TraceSpan> spans,
                               Predicate<Exception> isRetryableException) {
        this.table = table;
        this.insertStatement = insertStatement;
        this.spans = spans;
        this.isRetryableException = isRetryableException;
        this.isSummaryTable = table.equals(Tables.BITHON_TRACE_SPAN_SUMMARY.getName());
    }

    @Override
    public String getTableName() {
        return table;
    }

    @Override
    public int getInsertRows() {
        return spans.size();
    }

    @Override
    public void run(Connection connection) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(insertStatement)) {
            for (TraceSpan span : spans) {
                int col = 0;
                if (!isSummaryTable) {
                    statement.setTimestamp(++col, new Timestamp(span.startTime / 1000));
                }

                statement.setObject(++col, span.appName);
                statement.setObject(++col, span.instanceName);
                statement.setObject(++col, span.traceId);
                statement.setObject(++col, span.spanId);
                statement.setObject(++col, span.parentSpanId);
                statement.setObject(++col, StringUtils.getOrEmpty(span.name));
                statement.setObject(++col, StringUtils.getOrEmpty(span.clazz));
                statement.setObject(++col, StringUtils.getOrEmpty(span.method));
                statement.setObject(++col, StringUtils.getOrEmpty(span.kind));
                statement.setObject(++col, isSummaryTable ? fromUnixTimestampMicroseconds(span.startTime) : span.startTime);
                statement.setObject(++col, span.endTime);
                statement.setObject(++col, span.costTime);
                statement.setObject(++col, toTagStore(span.getTags()));
                statement.setObject(++col, span.getNormalizedUri());
                statement.setObject(++col, span.getStatus());
                statement.addBatch();
            }

            RetryUtils.retry(statement::executeBatch,
                             this::isExceptionRetryable,
                             3,
                             Duration.ofMillis(100));
        }
    }

    private LocalDateTime fromUnixTimestampMicroseconds(long microseconds) {
        Timestamp ts = new Timestamp(microseconds / 1000);
        ts.setNanos((int) (microseconds % 1000) * 1000);
        return ts.toLocalDateTime();
    }

    protected abstract Object toTagStore(Map<String, String> tag);

    private boolean isExceptionRetryable(Exception e) {
        return this.isRetryableException != null && this.isRetryableException.test(e);
    }
}
