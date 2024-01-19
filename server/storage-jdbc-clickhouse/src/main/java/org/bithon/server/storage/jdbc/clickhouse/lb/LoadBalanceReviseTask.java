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

import com.clickhouse.client.config.ClickHouseDefaults;
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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

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

    private Map<String, Collection<Shard>> shardSnapshot = Collections.emptyMap();

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

        Map<String, Collection<Shard>> shards = new HashMap<>();

        Properties props = new Properties();
        props.put(ClickHouseDefaults.USER.getKey(), this.clickHouseConfig.getUsername());
        props.put(ClickHouseDefaults.PASSWORD.getKey(), this.clickHouseConfig.getPassword());
        try (Connection connection = new JdbcDriver().connect(clickHouseConfig.getUrl(), props)) {

            Set<Integer> shardInCluster = getShards(connection);
            Map<String, Map<Integer, Shard>> tableSize = getTableSize(connection);

            // Manually join the two records
            for (Map.Entry<String, Map<Integer, Shard>> entry : tableSize.entrySet()) {
                String tableName = entry.getKey();
                Map<Integer, Shard> tableShards = entry.getValue();

                // Always perform checking even if the lengths of the arrays are the same
                for (int shard : shardInCluster) {
                    if (!tableShards.containsKey(shard)) {
                        tableShards.put(shard, new Shard(shard, 0, 0));
                    }
                }

                shards.put(tableName, tableShards.values());
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

    private Set<Integer> getShards(Connection connection) {
        Set<Integer> shards = new HashSet<>();
        try (PreparedStatement statement = connection.prepareStatement(StringUtils.format("SELECT distinct shard_num FROM system.clusters WHERE cluster = '%s'", this.clickHouseConfig.getCluster()))) {
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                shards.add(rs.getInt(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return shards;
    }

    private Map<String, Map<Integer, Shard>> getTableSize(Connection connection) {
        Map<String, Map<Integer, Shard>> records = new HashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(StringUtils.format("SELECT\n" +
                                                                                              "        table,\n" +
                                                                                              "        shardNum() as shard_num,\n" +
                                                                                              "        sum(bytes_on_disk) as bytes_on_disk,\n" +
                                                                                              "        sum(rows) as rows\n" +
                                                                                              "    FROM cluster('%s', system.parts) WHERE database = '%s' and active\n" +
                                                                                              "    GROUP BY table, shard_num\n" +
                                                                                              "    SETTINGS max_threads = 1\n",
                                                                                          clickHouseConfig.getCluster(),
                                                                                          clickHouseConfig.getDatabase()))) {
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                String table = rs.getString(1);
                int shard = rs.getInt(2);
                long size = rs.getLong(3);
                long rows = rs.getLong(4);

                records.computeIfAbsent(table, v -> new HashMap<>())
                       .put(shard, new Shard(shard, size, rows));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return records;
    }

    @Override
    protected void onException(Exception e) {
        log.error("Failed to update table size", e);
    }

    @Override
    protected void onStopped() {
    }
}
