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

package org.bithon.server.storage.jdbc.clickhouse.storage;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.sink.tracing.TraceSinkConfig;
import org.bithon.server.storage.common.ExpirationConfig;
import org.bithon.server.storage.common.IExpirationRunnable;
import org.bithon.server.storage.datasource.DataSourceSchemaManager;
import org.bithon.server.storage.datasource.typing.StringValueType;
import org.bithon.server.storage.jdbc.clickhouse.ClickHouseConfig;
import org.bithon.server.storage.jdbc.clickhouse.ClickHouseJooqContextHolder;
import org.bithon.server.storage.jdbc.clickhouse.ClickHouseSqlDialect;
import org.bithon.server.storage.jdbc.jooq.Tables;
import org.bithon.server.storage.jdbc.tracing.TraceJdbcReader;
import org.bithon.server.storage.jdbc.tracing.TraceJdbcStorage;
import org.bithon.server.storage.jdbc.tracing.TraceJdbcWriter;
import org.bithon.server.storage.jdbc.utils.SQLFilterBuilder;
import org.bithon.server.storage.metrics.IFilter;
import org.bithon.server.storage.tracing.ITraceReader;
import org.bithon.server.storage.tracing.ITraceWriter;
import org.bithon.server.storage.tracing.TraceSpan;
import org.bithon.server.storage.tracing.TraceStorageConfig;
import org.jooq.TableField;

import java.sql.Timestamp;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/10/27 9:34 下午
 */
@Slf4j
@JsonTypeName("clickhouse")
public class TraceStorage extends TraceJdbcStorage {

    private final ClickHouseConfig config;

    @JsonCreator
    public TraceStorage(@JacksonInject(useInput = OptBoolean.FALSE) ClickHouseJooqContextHolder dslContextHolder,
                        @JacksonInject(useInput = OptBoolean.FALSE) ClickHouseSqlDialect sqlDialect,
                        @JacksonInject(useInput = OptBoolean.FALSE) ObjectMapper objectMapper,
                        @JacksonInject(useInput = OptBoolean.FALSE) TraceStorageConfig storageConfig,
                        @JacksonInject(useInput = OptBoolean.FALSE) TraceSinkConfig traceConfig,
                        @JacksonInject(useInput = OptBoolean.FALSE) ClickHouseConfig config,
                        @JacksonInject(useInput = OptBoolean.FALSE) DataSourceSchemaManager schemaManager) {
        super(dslContextHolder.getDslContext(),
              objectMapper,
              storageConfig,
              traceConfig,
              schemaManager,
              sqlDialect);
        this.config = config;
    }

    @Override
    public void initialize() {
        TableCreator tableCreator = new TableCreator(config, dslContext);
        tableCreator.createIfNotExist(Tables.BITHON_TRACE_SPAN);
        tableCreator.createIfNotExist(Tables.BITHON_TRACE_SPAN_SUMMARY);
        tableCreator.createIfNotExist(Tables.BITHON_TRACE_MAPPING);
        tableCreator.createIfNotExist(Tables.BITHON_TRACE_SPAN_TAG_INDEX);
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
                DataCleaner cleaner = new DataCleaner(config, dslContext);
                cleaner.deleteFromPartition(Tables.BITHON_TRACE_SPAN.getName(), before);
                cleaner.deleteFromPartition(Tables.BITHON_TRACE_SPAN_SUMMARY.getName(), before);
                cleaner.deleteFromPartition(Tables.BITHON_TRACE_MAPPING.getName(), before);
                cleaner.deleteFromPartition(Tables.BITHON_TRACE_SPAN_TAG_INDEX.getName(), before);
            }
        };
    }

    @Override
    public ITraceWriter createWriter() {
        return new TraceJdbcWriter(dslContext, objectMapper, traceStorageConfig) {
            @Override
            protected boolean isTransactionSupported() {
                return false;
            }

            /**
             * The tag object is stored in
             * @return
             */
            @Override
            protected TableField getTagStoreField() {
                return Tables.BITHON_TRACE_SPAN.ATTRIBUTES;
            }

            /**
             * The map object is supported by ClickHouse JDBC, uses it directly
             */
            @Override
            protected Object toTagStore(TraceSpan.TagMap tag) {
                return tag;
            }
        };
    }

    @Override
    public ITraceReader createReader() {
        return new TraceJdbcReader(this.dslContext,
                                   this.objectMapper,
                                   this.traceSpanSchema,
                                   this.traceTagIndexSchema,
                                   this.traceStorageConfig,
                                   this.sqlDialect) {

            @Override
            protected String getTagPredicate(IFilter filter) {
                /*
                 * Use map accessor expression to search in the map
                 */
                String tag = StringUtils.format("%s['%s']", Tables.BITHON_TRACE_SPAN.ATTRIBUTES.getName(), filter.getName().substring(SPAN_TAGS_PREFIX.length()));
                return filter.getMatcher().accept(new SQLFilterBuilder(traceSpanSchema.getName(), tag, StringValueType.INSTANCE));
            }
        };
    }
}
