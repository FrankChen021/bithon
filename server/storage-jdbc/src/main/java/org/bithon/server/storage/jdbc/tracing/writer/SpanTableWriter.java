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
import org.bithon.server.storage.tracing.TraceSpan;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * @author Frank Chen
 * @date 16/1/24 8:04 pm
 */
public abstract class SpanTableWriter implements ITableWriter {
    private final String insertStatement;
    private final List<TraceSpan> spans;
    private final String table;
    private final Predicate<Exception> isRetryableException;

    public SpanTableWriter(String table,
                           String insertStatement,
                           List<TraceSpan> spans,
                           Predicate<Exception> isRetryableException) {
        this.table = table;
        this.insertStatement = insertStatement;
        this.spans = spans;
        this.isRetryableException = isRetryableException;
    }

    @Override
    public String getTable() {
        return table;
    }

    @Override
    public int getInsertSize() {
        return spans.size();
    }

    @Override
    public void run(Connection connection) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(insertStatement)) {
            for (TraceSpan span : spans) {
                statement.setObject(1, new Timestamp(span.startTime / 1000).toLocalDateTime());
                statement.setObject(2, span.appName);
                statement.setObject(3, span.instanceName);
                statement.setObject(4, span.traceId);
                statement.setObject(5, span.spanId);
                statement.setObject(6, span.parentSpanId);
                statement.setObject(7, StringUtils.getOrEmpty(span.name));
                statement.setObject(8, StringUtils.getOrEmpty(span.clazz));
                statement.setObject(9, StringUtils.getOrEmpty(span.method));
                statement.setObject(10, StringUtils.getOrEmpty(span.kind));
                statement.setObject(11, span.startTime);
                statement.setObject(12, span.endTime);
                statement.setObject(13, span.costTime);
                statement.setObject(14, toTagStore(span.getTags()));
                statement.setObject(15, span.getNormalizedUri());
                statement.setObject(16, span.getStatus());
                statement.addBatch();
            }

            RetryUtils.retry(statement::executeBatch,
                             this::isExceptionRetryable,
                             3,
                             Duration.ofMillis(100));
        }
    }

    protected abstract Object toTagStore(Map<String, String> tag);

    private boolean isExceptionRetryable(Exception e) {
        return this.isRetryableException != null && this.isRetryableException.test(e);
    }
}
