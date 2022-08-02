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

package org.bithon.server.storage.jdbc.clickhouse;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.time.DateTime;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.sink.tracing.TraceConfig;
import org.bithon.server.storage.datasource.DataSourceSchemaManager;
import org.bithon.server.storage.jdbc.jooq.Tables;
import org.bithon.server.storage.jdbc.jooq.tables.BithonTraceSpanSummary;
import org.bithon.server.storage.jdbc.tracing.TraceJdbcBatchWriter;
import org.bithon.server.storage.jdbc.tracing.TraceJdbcReader;
import org.bithon.server.storage.jdbc.tracing.TraceJdbcStorage;
import org.bithon.server.storage.jdbc.tracing.TraceJdbcWriter;
import org.bithon.server.storage.jdbc.utils.SQLFilterBuilder;
import org.bithon.server.storage.metrics.IFilter;
import org.bithon.server.storage.tracing.ITraceCleaner;
import org.bithon.server.storage.tracing.ITraceReader;
import org.bithon.server.storage.tracing.ITraceWriter;
import org.bithon.server.storage.tracing.TraceStorageConfig;
import org.jooq.Record1;
import org.jooq.SelectConditionStep;

import java.sql.Timestamp;
import java.util.List;
import java.util.stream.Collectors;

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
                        @JacksonInject(useInput = OptBoolean.FALSE) ObjectMapper objectMapper,
                        @JacksonInject(useInput = OptBoolean.FALSE) TraceStorageConfig storageConfig,
                        @JacksonInject(useInput = OptBoolean.FALSE) TraceConfig traceConfig,
                        @JacksonInject(useInput = OptBoolean.FALSE) ClickHouseConfig config,
                        @JacksonInject(useInput = OptBoolean.FALSE) DataSourceSchemaManager schemaManager) {
        super(dslContextHolder.getDslContext(), objectMapper, storageConfig, traceConfig, schemaManager);
        this.config = config;
    }

    @Override
    public void initialize() {
        TableCreator tableCreator = new TableCreator(config, dslContext);
        tableCreator.createIfNotExist(Tables.BITHON_TRACE_SPAN, config.getTtlDays());
        tableCreator.createIfNotExist(Tables.BITHON_TRACE_SPAN_SUMMARY, config.getTtlDays());
        tableCreator.createIfNotExist(Tables.BITHON_TRACE_MAPPING, config.getTtlDays());
        tableCreator.createIfNotExist(Tables.BITHON_TRACE_SPAN_TAG_INDEX, config.getTtlDays());
    }

    @Override
    public ITraceCleaner createCleaner() {
        return beforeTimestamp -> {
            String timestamp = DateTime.toYYYYMMDD(beforeTimestamp);

            DataCleaner cleaner = new DataCleaner(config, dslContext);
            cleaner.clean(Tables.BITHON_TRACE_SPAN.getName(), timestamp);
            cleaner.clean(Tables.BITHON_TRACE_SPAN_SUMMARY.getName(), timestamp);
            cleaner.clean(Tables.BITHON_TRACE_MAPPING.getName(), timestamp);
            cleaner.clean(Tables.BITHON_TRACE_SPAN_TAG_INDEX.getName(), timestamp);
        };
    }

    @Override
    public ITraceWriter createWriter() {
        return new TraceJdbcBatchWriter(new TraceJdbcWriter(dslContext, objectMapper, traceConfig) {
            @Override
            protected boolean isTransactionSupported() {
                return false;
            }
        }, super.config);
    }

    @Override
    public ITraceReader createReader() {
        return new TraceJdbcReader(this.dslContext, this.objectMapper, traceSpanSchema, traceConfig) {
            @Override
            public List<Histogram> getTraceDistribution(List<IFilter> filters, Timestamp start, Timestamp end) {
                BithonTraceSpanSummary summaryTable = Tables.BITHON_TRACE_SPAN_SUMMARY;

                StringBuilder sqlBuilder = new StringBuilder(StringUtils.format("SELECT arrayJoin(histogram(100)(%s)) AS histogram FROM %s",
                                                                                summaryTable.COSTTIMEMS.getName(),
                                                                                summaryTable.getQualifiedName()));
                sqlBuilder.append(StringUtils.format(" WHERE %s >= '%s' AND %s < '%s'",
                                                     summaryTable.TIMESTAMP.getName(),
                                                     DateTime.toYYYYMMDDhhmmss(start.getTime()),
                                                     summaryTable.TIMESTAMP.getName(),
                                                     DateTime.toYYYYMMDDhhmmss(end.getTime())));

                String moreFilter = SQLFilterBuilder.build(traceSpanSchema,
                                                           filters.stream().filter(filter -> !filter.getName().startsWith(SPAN_TAGS_PREFIX)));
                if (StringUtils.hasText(moreFilter)) {
                    sqlBuilder.append(" AND ");
                    sqlBuilder.append(moreFilter);
                }

                // build tag query
                SelectConditionStep<Record1<String>> tagQuery = buildTagQuery(start, end, filters);
                if (tagQuery != null) {
                    sqlBuilder.append(" AND ");
                    sqlBuilder.append(dslContext.renderInlined(summaryTable.TRACEID.in(tagQuery)));
                }

                String finalSqlBuilder = "SELECT histogram.1 AS lower, histogram.2 AS upper, histogram.3 AS height FROM ("
                                         + sqlBuilder
                                         + " )";

                List<Histogram> histograms = dslContext.fetch(finalSqlBuilder).into(Histogram.class);

                //
                // convert to height to percentage.
                // The final value is multiplied 100 times
                // so the value is in range [1, 10000]
                //
                double total = 0;
                for (Histogram histogram : histograms) {
                    total += histogram.getHeight();
                }
                for (Histogram histogram : histograms) {
                    histogram.setHeight((int) (histogram.getHeight() / total * 10000));
                }
                return histograms.stream().filter(histogram -> histogram.getHeight() > 0).collect(Collectors.toList());
            }
        };
    }
}
