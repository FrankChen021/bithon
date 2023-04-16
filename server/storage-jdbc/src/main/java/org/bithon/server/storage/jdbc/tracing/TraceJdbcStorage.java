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
import org.bithon.server.sink.tracing.TraceSinkConfig;
import org.bithon.server.storage.common.ExpirationConfig;
import org.bithon.server.storage.common.IExpirationRunnable;
import org.bithon.server.storage.datasource.DataSourceSchema;
import org.bithon.server.storage.datasource.DataSourceSchemaManager;
import org.bithon.server.storage.jdbc.JdbcJooqContextHolder;
import org.bithon.server.storage.jdbc.jooq.Tables;
import org.bithon.server.storage.jdbc.metric.ISqlDialect;
import org.bithon.server.storage.tracing.ITraceReader;
import org.bithon.server.storage.tracing.ITraceStorage;
import org.bithon.server.storage.tracing.ITraceWriter;
import org.bithon.server.storage.tracing.TraceStorageConfig;
import org.jooq.DSLContext;

import java.sql.Timestamp;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/4 8:34 下午
 */
@Slf4j
@JsonTypeName("jdbc")
public class TraceJdbcStorage implements ITraceStorage {


    protected final DSLContext dslContext;
    protected final ObjectMapper objectMapper;
    protected final TraceStorageConfig traceStorageConfig;
    protected final TraceSinkConfig traceSinkConfig;
    protected final DataSourceSchema traceSpanSchema;
    protected final DataSourceSchema traceTagIndexSchema;

    @JsonCreator
    public TraceJdbcStorage(@JacksonInject(useInput = OptBoolean.FALSE) JdbcJooqContextHolder dslContextHolder,
                            @JacksonInject(useInput = OptBoolean.FALSE) ObjectMapper objectMapper,
                            @JacksonInject(useInput = OptBoolean.FALSE) TraceStorageConfig storageConfig,
                            @JacksonInject(useInput = OptBoolean.FALSE) TraceSinkConfig traceSinkConfig,
                            @JacksonInject(useInput = OptBoolean.FALSE) DataSourceSchemaManager schemaManager) {
        this(dslContextHolder.getDslContext(), objectMapper, storageConfig, traceSinkConfig, schemaManager);
    }

    public TraceJdbcStorage(DSLContext dslContext,
                            ObjectMapper objectMapper,
                            TraceStorageConfig storageConfig,
                            TraceSinkConfig traceSinkConfig,
                            DataSourceSchemaManager schemaManager) {
        this.dslContext = dslContext;
        this.objectMapper = objectMapper;
        this.traceStorageConfig = storageConfig;
        this.traceSinkConfig = traceSinkConfig;
        this.traceSpanSchema = schemaManager.getDataSourceSchema("trace_span_summary");
        this.traceTagIndexSchema = schemaManager.getDataSourceSchema("trace_span_tag_index");
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
        return new TraceJdbcWriter(dslContext, objectMapper, traceStorageConfig);
    }

    @Override
    public ITraceReader createReader() {
        return new TraceJdbcReader(this.dslContext,
                                   this.objectMapper,
                                   this.traceSpanSchema,
                                   this.traceTagIndexSchema,
                                   this.traceStorageConfig);
    }

    @Override
    public IExpirationRunnable getExpirationRunnable() {
        return new IExpirationRunnable() {
            @Override
            public ExpirationConfig getRule() {
                return traceStorageConfig.getTtl();
            }

            @Override
            public void expire(Timestamp before) {
                dslContext.deleteFrom(Tables.BITHON_TRACE_SPAN)
                          .where(Tables.BITHON_TRACE_SPAN.TIMESTAMP.le(before.toLocalDateTime()))
                          .execute();

                dslContext.deleteFrom(Tables.BITHON_TRACE_SPAN_SUMMARY)
                          .where(Tables.BITHON_TRACE_SPAN_SUMMARY.TIMESTAMP.le(before.toLocalDateTime()))
                          .execute();

                dslContext.deleteFrom(Tables.BITHON_TRACE_MAPPING)
                          .where(Tables.BITHON_TRACE_MAPPING.TIMESTAMP.le(before.toLocalDateTime()))
                          .execute();

                dslContext.deleteFrom(Tables.BITHON_TRACE_SPAN_TAG_INDEX)
                          .where(Tables.BITHON_TRACE_SPAN_TAG_INDEX.TIMESTAMP.le(before.toLocalDateTime()))
                          .execute();
            }
        };
    }

}
