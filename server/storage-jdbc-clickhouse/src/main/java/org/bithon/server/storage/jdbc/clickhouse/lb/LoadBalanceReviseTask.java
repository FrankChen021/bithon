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

package org.bithon.server.storage.jdbc.clickhouse.lb;

import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.concurrency.PeriodicTask;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.storage.jdbc.clickhouse.ClickHouseConfig;
import org.bithon.server.storage.jdbc.clickhouse.JdbcDriver;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The task that updates the information for client side balancing.
 * This is a supplementary to revise the client side statistics.
 *
 * @author frank.chen021@outlook.com
 * @date 2024/1/14 12:32
 */
@Slf4j
public class LoadBalanceReviseTask extends PeriodicTask {
    private static volatile LoadBalanceReviseTask instance;

    public static LoadBalanceReviseTask getInstance(ClickHouseConfig clickHouseConfig) {
        if (instance == null) {
            synchronized (LoadBalanceReviseTask.class) {
                if (instance != null) {
                    return instance;
                }
                instance = new LoadBalanceReviseTask(clickHouseConfig);
                instance.start();
            }
        }
        return instance;
    }

    private final ClickHouseConfig clickHouseConfig;
    private final List<IShardsUpdateListener> listeners = new ArrayList<>();

    private Map<String, List<Shard>> shardSnapshot = Collections.emptyMap();

    private LoadBalanceReviseTask(ClickHouseConfig clickHouseConfig) {
        super("size-update", Duration.ofMinutes(5), true);
        this.clickHouseConfig = clickHouseConfig;
    }

    public void addListener(IShardsUpdateListener listener) {
        if (listener != null) {
            synchronized (listeners) {
                listeners.add(listener);
            }
            listener.update(this.shardSnapshot);
        }
    }

    public void removeListener(IShardsUpdateListener listener) {
        if (listener != null) {
            synchronized (listeners) {
                listeners.remove(listener);
            }
        }
    }

    @Override
    protected void onRun() throws Exception {
        log.info("Get the table size under database [{}] for client side load balancing", clickHouseConfig.getDatabase());

        Map<String, List<Shard>> shards = new HashMap<>();

        try (Connection connection = new JdbcDriver().connect(clickHouseConfig.getUrl())) {
            String sql = StringUtils.format("SELECT size.table AS table, shard_num, bytes_on_disk, rows FROM\n" +
                                                "(\n" +
                                                "    SELECT distinct shard_num FROM system.clusters WHERE cluster = '%s'\n" +
                                                ") shards\n" +
                                                "LEFT JOIN (\n" +
                                                "    SELECT\n" +
                                                "        table,\n" +
                                                "        shardNum() as shard_num,\n" +
                                                "        sum(bytes_on_disk) as bytes_on_disk,\n" +
                                                "        sum(rows) as rows\n" +
                                                "    FROM cluster('%s', system.parts) WHERE database = '%s' and active\n" +
                                                "    GROUP BY table, shard_num\n" +
                                                "    SETTINGS max_threads = 1\n" +
                                                ") size\n" +
                                                "ON size.shard_num = shards.shard_num\n" +
                                                "ORDER BY table, shard_num",
                                            clickHouseConfig.getCluster(),
                                            clickHouseConfig.getCluster(),
                                            clickHouseConfig.getDatabase());

            try (PreparedStatement statement = connection.prepareStatement(sql)) {

                ResultSet rs = statement.executeQuery();
                while (rs.next()) {
                    String table = rs.getString(1);
                    int shard = rs.getInt(2);
                    long size = rs.getLong(3);
                    long rows = rs.getLong(4);

                    shards.computeIfAbsent(table, v -> new ArrayList<>())
                          .add(new Shard(shard, size, rows));
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        synchronized (listeners) {
            for (IShardsUpdateListener listener : listeners) {
                try {
                    listener.update(shards);
                } catch (Exception ignored) {
                }
            }
        }

        shardSnapshot = shards;
    }

    @Override
    protected void onException(Exception e) {
        log.error("Failed to update table size", e);
    }

    @Override
    protected void onStopped() {
    }
}
