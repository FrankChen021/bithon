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
import org.bithon.component.commons.expression.ComparisonExpression;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.IdentifierExpression;
import org.bithon.component.commons.expression.LiteralExpression;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.sink.tracing.TraceSinkConfig;
import org.bithon.server.storage.common.ExpirationConfig;
import org.bithon.server.storage.common.IExpirationRunnable;
import org.bithon.server.storage.datasource.DataSourceSchemaManager;
import org.bithon.server.storage.jdbc.clickhouse.ClickHouseConfig;
import org.bithon.server.storage.jdbc.clickhouse.ClickHouseStorageConfiguration;
import org.bithon.server.storage.jdbc.jooq.Tables;
import org.bithon.server.storage.jdbc.tracing.TraceJdbcReader;
import org.bithon.server.storage.jdbc.tracing.TraceJdbcStorage;
import org.bithon.server.storage.jdbc.tracing.TraceJdbcWriter;
import org.bithon.server.storage.jdbc.utils.Expression2Sql;
import org.bithon.server.storage.jdbc.utils.SqlDialectManager;
import org.bithon.server.storage.tracing.ITraceReader;
import org.bithon.server.storage.tracing.ITraceWriter;
import org.bithon.server.storage.tracing.TraceStorageConfig;

import java.sql.Timestamp;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/10/27 9:34 下午
 */
@Slf4j
@JsonTypeName("clickhouse")
public class TraceStorage extends TraceJdbcStorage {

    private final ClickHouseConfig config;

    @JsonCreator
    public TraceStorage(@JacksonInject(useInput = OptBoolean.FALSE) ClickHouseStorageConfiguration configuration,
                        @JacksonInject(useInput = OptBoolean.FALSE) ObjectMapper objectMapper,
                        @JacksonInject(useInput = OptBoolean.FALSE) TraceStorageConfig storageConfig,
                        @JacksonInject(useInput = OptBoolean.FALSE) TraceSinkConfig traceConfig,
                        @JacksonInject(useInput = OptBoolean.FALSE) DataSourceSchemaManager schemaManager,
                        @JacksonInject(useInput = OptBoolean.FALSE) SqlDialectManager sqlDialectManager) {
        super(configuration.getDslContext(),
              objectMapper,
              storageConfig,
              traceConfig,
              schemaManager,
              sqlDialectManager);
        this.config = configuration.getClickHouseConfig();
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
             * The map object is supported by ClickHouse JDBC, uses it directly
             */
            @Override
            protected Object toTagStore(Map<String, String> tag) {
                // TagMap is an instance of java.util.Map,
                // can be directly returned since ClickHouse JDBC supports such type
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
                                   this.sqlDialectManager.getSqlDialect(this.dslContext)) {

            /**
             * In ClickHouse, the tags are stored in a Map field.
             * We need to use map accessor expression to search in the map
             */
            @Override
            protected String getTagPredicate(IExpression tagFilter) {
                if (!(tagFilter instanceof ComparisonExpression)) {
                    throw new UnsupportedOperationException(StringUtils.format("[%s] matcher on tag field is not supported on this database.",
                                                                               tagFilter.getClass().getSimpleName()));
                }

                IExpression left = ((ComparisonExpression.EQ) tagFilter).getLeft();
                IExpression right = ((ComparisonExpression.EQ) tagFilter).getRight();
                if (!(left instanceof IdentifierExpression)) {
                    throw new UnsupportedOperationException(StringUtils.format("The left operator in expression [%s] should be identifier only.",
                                                                               tagFilter.serializeToText()));
                }
                if (!(right instanceof LiteralExpression)) {
                    throw new UnsupportedOperationException(StringUtils.format("The right operator in expression [%s] should be literal only.",
                                                                               tagFilter.serializeToText()));
                }

                String tag = StringUtils.format("%s['%s']", Tables.BITHON_TRACE_SPAN.ATTRIBUTES.getName(), ((IdentifierExpression) left).getIdentifier().substring(SPAN_TAGS_PREFIX.length()));
                ((IdentifierExpression) left).setIdentifier(tag);

                return Expression2Sql.from(null/*do not use qualified name*/, sqlDialect, tagFilter, false);
            }

            @Override
            protected Map<String, String> toTagMap(Object attributes) {
                //noinspection unchecked
                return (Map<String, String>) attributes;
            }
        };
    }
}
