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
import org.bithon.server.storage.jdbc.metric.MetricJdbcWriter;
import org.bithon.server.storage.jdbc.metric.MetricTable;
import org.jooq.DSLContext;

import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * The writer that implements a client side load balancing which would greatly reduce
 *
 * @author frank.chen021@outlook.com
 * @date 2024/1/14 11:13
 */
@Slf4j
public class LoadBalancedMetricWriter extends MetricJdbcWriter implements IShardsUpdateListener {

    private final ClickHouseConfig clickHouseConfig;
    private final ILoadBalancer loadBalancer;
    private final String serverUrl;

    public LoadBalancedMetricWriter(DSLContext dslContext,
                                    ClickHouseConfig clickHouseConfig,
                                    MetricTable table) {
        super(dslContext, table, false, RetryableExceptions::isExceptionRetryable);

        this.clickHouseConfig = clickHouseConfig;
        this.loadBalancer = new LeastRowsLoadBalancer();

        String url = clickHouseConfig.getUrl();
        if (url.lastIndexOf('?') == -1) {
            // The URL has param
            url += "?";
        }
        this.serverUrl = url;

        LoadBalanceReviseTask task = LoadBalanceReviseTask.getInstance(clickHouseConfig);
        task.addListener(this);
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

    @Override
    protected void doInsert(IOnceTableWriter writer) throws Throwable {
        int shard = this.loadBalancer.nextShard(writer.getInsertSize());

        Properties props = new Properties();
        props.put("user", this.clickHouseConfig.getUsername());
        props.put("password", this.clickHouseConfig.getPassword());
        try (Connection connection = new JdbcDriver().connect(StringUtils.format("%s&custom_http_params=insert_shard_id=%d",
                                                                                 this.serverUrl,
                                                                                 shard),
                                                              props)) {
            writer.run(connection);
        }

        log.info("Flushed {} rows to {} on shard {}", writer.getInsertSize(), table.getName(), shard);
    }
}
