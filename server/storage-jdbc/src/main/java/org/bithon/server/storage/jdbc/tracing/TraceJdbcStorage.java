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

package org.bithon.server.storage.jdbc.tracing;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.bithon.server.sink.tracing.TraceConfig;
import org.bithon.server.storage.datasource.DataSourceSchema;
import org.bithon.server.storage.datasource.DataSourceSchemaManager;
import org.bithon.server.storage.jdbc.jooq.Tables;
import org.bithon.server.storage.tracing.ITraceCleaner;
import org.bithon.server.storage.tracing.ITraceReader;
import org.bithon.server.storage.tracing.ITraceStorage;
import org.bithon.server.storage.tracing.ITraceWriter;
import org.bithon.server.storage.tracing.TraceStorageConfig;
import org.jooq.DSLContext;

import java.sql.Timestamp;

import static org.bithon.server.storage.jdbc.JdbcStorageAutoConfiguration.BITHON_JDBC_DSL;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/4 8:34 下午
 */
@Slf4j
@JsonTypeName("jdbc")
public class TraceJdbcStorage implements ITraceStorage {


    protected final DSLContext dslContext;
    protected final ObjectMapper objectMapper;
    protected final TraceStorageConfig config;
    protected final TraceConfig traceConfig;
    protected final DataSourceSchema traceSpanSchema;

    @JsonCreator
    public TraceJdbcStorage(@JacksonInject(value = BITHON_JDBC_DSL, useInput = OptBoolean.FALSE) DSLContext dslContext,
                            @JacksonInject(useInput = OptBoolean.FALSE) ObjectMapper objectMapper,
                            @JacksonInject(useInput = OptBoolean.FALSE) TraceStorageConfig storageConfig,
                            @JacksonInject(useInput = OptBoolean.FALSE) TraceConfig traceConfig,
                            @JacksonInject(useInput = OptBoolean.FALSE)DataSourceSchemaManager schemaManager) {
        this.dslContext = dslContext;
        this.objectMapper = objectMapper;
        this.config = storageConfig;
        this.traceConfig = traceConfig;
        this.traceSpanSchema = schemaManager.getDataSourceSchema("trace_span_summary");
    }

    @Override
    public void initialize() {
        dslContext.createTableIfNotExists(Tables.BITHON_TRACE_SPAN)
                  .columns(Tables.BITHON_TRACE_SPAN.fields())
                  .indexes(Tables.BITHON_TRACE_SPAN.getIndexes())
                  .execute();
        dslContext.createTableIfNotExists(Tables.BITHON_TRACE_SPAN_SUMMARY)
                  .columns(Tables.BITHON_TRACE_SPAN_SUMMARY.fields())
                  .indexes(Tables.BITHON_TRACE_SPAN_SUMMARY.getIndexes())
                  .execute();
        dslContext.createTableIfNotExists(Tables.BITHON_TRACE_MAPPING)
                  .columns(Tables.BITHON_TRACE_MAPPING.fields())
                  .indexes(Tables.BITHON_TRACE_MAPPING.getIndexes())
                  .execute();
        dslContext.createTableIfNotExists(Tables.BITHON_TRACE_SPAN_TAG_INDEX)
                  .columns(Tables.BITHON_TRACE_SPAN_TAG_INDEX.fields())
                  .indexes(Tables.BITHON_TRACE_SPAN_TAG_INDEX.getIndexes())
                  .execute();
    }

    @Override
    public ITraceWriter createWriter() {
        return new TraceJdbcBatchWriter(new TraceJdbcWriter(dslContext, objectMapper, traceConfig), config);
    }

    @Override
    public ITraceReader createReader() {
        return new TraceJdbcReader(this.dslContext, this.objectMapper, this.traceSpanSchema, this.traceConfig);
    }

    @Override
    public ITraceCleaner createCleaner() {
        return beforeTimestamp -> {
            Timestamp before = new Timestamp(beforeTimestamp);

            dslContext.deleteFrom(Tables.BITHON_TRACE_SPAN)
                      .where(Tables.BITHON_TRACE_SPAN.TIMESTAMP.le(before))
                      .execute();

            dslContext.deleteFrom(Tables.BITHON_TRACE_SPAN_SUMMARY)
                      .where(Tables.BITHON_TRACE_SPAN_SUMMARY.TIMESTAMP.le(before))
                      .execute();

            dslContext.deleteFrom(Tables.BITHON_TRACE_MAPPING)
                      .where(Tables.BITHON_TRACE_MAPPING.TIMESTAMP.le(before))
                      .execute();

            dslContext.deleteFrom(Tables.BITHON_TRACE_SPAN_TAG_INDEX)
                      .where(Tables.BITHON_TRACE_SPAN_TAG_INDEX.TIMESTAMP.le(before))
                      .execute();
        };
    }

}
