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

import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.storage.jdbc.clickhouse.ClickHouseConfig;
import org.bithon.server.storage.jdbc.clickhouse.JdbcDriver;
import org.bithon.server.storage.jdbc.clickhouse.common.exception.RetryableExceptions;
import org.bithon.server.storage.jdbc.clickhouse.lb.ILoadBalancer;
import org.bithon.server.storage.jdbc.clickhouse.lb.IShardsUpdateListener;
import org.bithon.server.storage.jdbc.clickhouse.lb.LeastRowsLoadBalancer;
import org.bithon.server.storage.jdbc.clickhouse.lb.LoadBalanceReviseTask;
import org.bithon.server.storage.jdbc.clickhouse.lb.Shard;
import org.bithon.server.storage.jdbc.common.IOnceTableWriter;
import org.bithon.server.storage.jdbc.common.jooq.Tables;
import org.bithon.server.storage.jdbc.tracing.writer.SpanTableWriter;
import org.bithon.server.storage.jdbc.tracing.writer.TraceJdbcWriter;
import org.bithon.server.storage.tracing.TraceSpan;
import org.bithon.server.storage.tracing.TraceStorageConfig;
import org.jooq.DSLContext;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * @author frank.chen021@outlook.com
 * @date 2024/1/15 20:21
 */
@Slf4j
class LoadBalancedTraceWriter extends TraceJdbcWriter implements IShardsUpdateListener {

    private final ILoadBalancer summaryTableLoadBalancer;
    private final ILoadBalancer spanTableLoadBalancer;
    private final ILoadBalancer indexTableLoadBalancer;
    private final ILoadBalancer mappingTableLoadBalancer;

    private final ClickHouseConfig clickHouseConfig;
    private final String serverUrl;

    LoadBalancedTraceWriter(ClickHouseConfig clickHouseConfig,
                            TraceStorageConfig traceStorageConfig,
                            DSLContext dslContext) {
        super(dslContext, traceStorageConfig, RetryableExceptions::isExceptionRetryable);

        this.clickHouseConfig = clickHouseConfig;

        this.summaryTableLoadBalancer = new LeastRowsLoadBalancer();
        this.spanTableLoadBalancer = new LeastRowsLoadBalancer();
        this.indexTableLoadBalancer = new LeastRowsLoadBalancer();
        this.mappingTableLoadBalancer = new LeastRowsLoadBalancer();

        String url = clickHouseConfig.getUrl();
        if (url.lastIndexOf('?') == -1) {
            // The URL has param
            url += "?";
        }
        serverUrl = url;

        LoadBalanceReviseTask.getInstance(clickHouseConfig)
                             .addListener(this);
    }

    @Override
    public void update(Map<String, List<Shard>> shards) {
        String summaryTable = clickHouseConfig.getLocalTableName(Tables.BITHON_TRACE_SPAN_SUMMARY.getName());
        this.summaryTableLoadBalancer.update(shards.get(summaryTable));

        String spanTable = clickHouseConfig.getLocalTableName(Tables.BITHON_TRACE_SPAN.getName());
        this.spanTableLoadBalancer.update(shards.get(spanTable));

        String mappingTable = clickHouseConfig.getLocalTableName(Tables.BITHON_TRACE_MAPPING.getName());
        this.mappingTableLoadBalancer.update(shards.get(mappingTable));

        String indexTable = clickHouseConfig.getLocalTableName(Tables.BITHON_TRACE_SPAN_TAG_INDEX.getName());
        this.indexTableLoadBalancer.update(shards.get(indexTable));
    }

    @Override
    public void close() {
        LoadBalanceReviseTask.getInstance(null)
                             .removeListener(this);
    }

    @Override
    protected void doInsert(IOnceTableWriter writer) throws Throwable {
        ILoadBalancer loadBalancer;
        if (writer.getTable().equals(Tables.BITHON_TRACE_SPAN.getName())) {
            loadBalancer = spanTableLoadBalancer;
        } else if (writer.getTable().equals(Tables.BITHON_TRACE_SPAN_SUMMARY.getName())) {
            loadBalancer = summaryTableLoadBalancer;
        } else if (writer.getTable().equals(Tables.BITHON_TRACE_MAPPING.getName())) {
            loadBalancer = mappingTableLoadBalancer;
        } else if (writer.getTable().equals(Tables.BITHON_TRACE_SPAN_TAG_INDEX.getName())) {
            loadBalancer = indexTableLoadBalancer;
        } else {
            throw new RuntimeException("Not supported table:" + writer.getTable());
        }

        int shard = loadBalancer.nextShard(writer.getInsertSize());

        Properties props = new Properties();
        props.put("user", this.clickHouseConfig.getUsername());
        props.put("password", this.clickHouseConfig.getPassword());
        try (Connection connection = new JdbcDriver().connect(StringUtils.format("%s&custom_http_params=insert_shard_id=%d",
                                                                                 this.serverUrl,
                                                                                 shard),
                                                              props)) {
            writer.run(connection);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        log.info("Flushed {} rows to {} on shard {}", writer.getInsertSize(), writer.getTable(), shard);
    }

    @Override
    protected IOnceTableWriter createInsertSpanRunnable(String table, String insertStatement, List<TraceSpan> spans) {
        return new SpanTableWriter(table, insertStatement, spans, this.isRetryableException) {
            @Override
            protected Object toTagStore(Map<String, String> tag) {
                return tag;
            }
        };
    }
}
