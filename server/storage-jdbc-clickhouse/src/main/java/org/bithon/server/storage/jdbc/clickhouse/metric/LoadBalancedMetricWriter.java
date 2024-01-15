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

package org.bithon.server.storage.jdbc.clickhouse.metric;

import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.utils.CollectionUtils;
import org.bithon.component.commons.utils.RetryUtils;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.storage.datasource.input.IInputRow;
import org.bithon.server.storage.jdbc.clickhouse.ClickHouseConfig;
import org.bithon.server.storage.jdbc.clickhouse.JdbcDriver;
import org.bithon.server.storage.jdbc.clickhouse.exception.RetryableExceptions;
import org.bithon.server.storage.jdbc.clickhouse.lb.ILoadBalancer;
import org.bithon.server.storage.jdbc.clickhouse.lb.IShardsUpdateListener;
import org.bithon.server.storage.jdbc.clickhouse.lb.LeastRowsLoadBalancer;
import org.bithon.server.storage.jdbc.clickhouse.lb.LoadBalanceReviseTask;
import org.bithon.server.storage.jdbc.clickhouse.lb.Shard;
import org.bithon.server.storage.jdbc.metric.MetricTable;
import org.bithon.server.storage.metrics.IMetricWriter;
import org.jooq.Field;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * The writer that implements a client side load balancing which would greatly reduce
 *
 * @author frank.chen021@outlook.com
 * @date 2024/1/14 11:13
 */
@Slf4j
public class LoadBalancedMetricWriter implements IMetricWriter, IShardsUpdateListener {

    private final MetricTable table;
    private final ClickHouseConfig clickHouseConfig;
    private final ILoadBalancer loadBalancer;
    private final String insertStatement;

    public LoadBalancedMetricWriter(ClickHouseConfig clickHouseConfig, MetricTable table) {
        this.clickHouseConfig = clickHouseConfig;
        this.table = table;
        this.loadBalancer = new LeastRowsLoadBalancer();

        int fieldCount = 1 + table.getDimensions().size() + table.getMetrics().size();
        StringBuilder sb = new StringBuilder(512);
        sb.append("INSERT INTO ");
        sb.append(clickHouseConfig.getDatabase());
        sb.append(".");
        sb.append(table.getName());

        // column names
        sb.append(" (");
        sb.append("timestamp");
        for (Field<?> dim : table.getDimensions()) {
            sb.append(", ");
            sb.append(dim.getName());
        }
        for (Field<?> metric : table.getMetrics()) {
            sb.append(", ");
            sb.append(metric.getName());
        }
        sb.append(" )");

        // placeholders
        sb.append(" VALUES (");
        for (int i = 0; i < fieldCount; i++) {
            sb.append("?,");
        }
        sb.deleteCharAt(sb.length() - 1);
        sb.append(")");
        this.insertStatement = sb.toString();

        LoadBalanceReviseTask task = LoadBalanceReviseTask.getInstance(clickHouseConfig);
        task.addListener(this);
    }

    @Override
    public void write(List<IInputRow> inputRowList) {
        if (CollectionUtils.isEmpty(inputRowList)) {
            return;
        }

        int shard = this.loadBalancer.nextShard(inputRowList.size());

        String url = clickHouseConfig.getUrl();
        if (url.lastIndexOf('?') == -1) {
            // The URL has param
            url += "?";
        }

        try (Connection connection = new JdbcDriver().connect(StringUtils.format("%s&custom_http_params=insert_shard_id=%d",
                                                                                 url,
                                                                                 shard))) {
            try (PreparedStatement statement = connection.prepareStatement(this.insertStatement)) {

                for (IInputRow inputRow : inputRowList) {
                    int index = 1;
                    statement.setObject(index++, new Timestamp(inputRow.getColAsLong("timestamp")));

                    // dimensions
                    for (Field<?> dimension : table.getDimensions()) {
                        // the value might be type of integer, so Object should be used
                        Object value = inputRow.getCol(dimension.getName(), "");
                        statement.setObject(index++, value.toString());
                    }

                    // metrics
                    for (Field<?> metric : table.getMetrics()) {
                        statement.setObject(index++, inputRow.getCol(metric.getName(), 0));
                    }
                }
                statement.addBatch();

                RetryUtils.retry(statement::executeBatch,
                                 RetryableExceptions::isExceptionRetryable,
                                 3,
                                 Duration.ofMillis(100));

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        log.trace("Flushed {} rows to {} on shard {}", inputRowList.size(), table.getName(), shard);
    }

    @Override
    public void close() {
        LoadBalanceReviseTask.getInstance(clickHouseConfig).removeListener(this);
    }

    @Override
    public void update(Map<String, List<Shard>> shards) {
        String localTable = clickHouseConfig.getLocalTableName(table.getName());
        this.loadBalancer.update(shards.get(localTable));
    }
}
