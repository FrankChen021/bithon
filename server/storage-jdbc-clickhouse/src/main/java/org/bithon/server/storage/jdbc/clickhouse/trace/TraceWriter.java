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

package org.bithon.server.storage.jdbc.clickhouse.trace;

import org.bithon.server.storage.jdbc.clickhouse.common.exception.RetryableExceptions;
import org.bithon.server.storage.jdbc.tracing.writer.SpanTableJdbcWriter;
import org.bithon.server.storage.jdbc.tracing.writer.TraceJdbcWriter;
import org.bithon.server.storage.tracing.TraceSpan;
import org.bithon.server.storage.tracing.TraceStorageConfig;
import org.jooq.DSLContext;

import java.util.List;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2024/4/27 19:11
 */
class TraceWriter extends TraceJdbcWriter {

    public TraceWriter(TraceStorageConfig traceStorageConfig,
                       DSLContext dslContext) {
        super(dslContext, traceStorageConfig, RetryableExceptions::isExceptionRetryable);
    }

    @Override
    protected boolean isTransactionSupported() {
        return false;
    }

    @Override
    protected boolean isWriteSummaryTable() {
        return false;
    }

    @Override
    protected SpanTableJdbcWriter createInsertSpanRunnable(String table, String insertStatement, List<TraceSpan> spans) {
        return new SpanTableJdbcWriter(table, insertStatement, spans, this.isRetryableException) {
            /**
             * The map object is supported by ClickHouse JDBC, uses it directly
             */
            @Override
            protected Object toTagStore(Map<String, String> tag) {
                // TagMap is an instance of java.util.Map,
                // can be directly returned since ClickHouse JDBC supports such a type
                return tag;
            }
        };
    }
}
