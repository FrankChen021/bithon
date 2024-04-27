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

import com.clickhouse.client.ClickHouseNode;
import com.clickhouse.client.config.ClickHouseClientOption;
import com.clickhouse.data.ClickHouseColumn;
import com.clickhouse.data.ClickHouseVersion;
import com.clickhouse.jdbc.ClickHouseConnection;
import com.clickhouse.jdbc.ClickHouseDriver;
import com.clickhouse.jdbc.JdbcConfig;
import com.clickhouse.jdbc.internal.ClickHouseConnectionImpl;
import com.clickhouse.jdbc.internal.ClickHouseJdbcUrlParser;
import lombok.AllArgsConstructor;
import org.bithon.component.commons.utils.RetryUtils;
import org.bithon.server.storage.jdbc.clickhouse.common.exception.RetryableExceptions;

import java.sql.SQLException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author frank.chen021@outlook.com
 * @date 2024/1/13 17:55
 */
public class JdbcDriver extends ClickHouseDriver {

    /**
     * A global cache for the INSERT statements.
     * The key is the table name.
     */
    private static final Map<String, Map<String, List<ClickHouseColumn>>> COLUMN_LIST_CACHE = new ConcurrentHashMap<>();

    @AllArgsConstructor
    static class ServerInfo {
        private TimeZone timeZone;
        private ClickHouseVersion version;
    }

    private static final Map<String, ServerInfo> SERVER_INFO_CACHE = new ConcurrentHashMap<>();

    @Override
    public ClickHouseConnection connect(String url, Properties info) throws SQLException {
        if (!acceptsURL(url)) {
            return null;
        }

        if (info == null) {
            info = new Properties();
        }
        // Set default options
        info.put(JdbcConfig.PROP_NULL_AS_DEFAULT, "1");
        info.put(ClickHouseClientOption.COMPRESS.getKey(), true);
        info.put(ClickHouseClientOption.DECOMPRESS.getKey(), true);

        // Cache necessary server properties
        // so that we reduce the requests to the server when a new connection is established
        ClickHouseJdbcUrlParser.ConnectionInfo connectionInfo = ClickHouseJdbcUrlParser.parse(url, info);
        ClickHouseNode node = connectionInfo.getNodes().getNodes().get(0);
        String host = node.getHost() + ":" + node.getPort();
        ServerInfo cachedServerInfo = SERVER_INFO_CACHE.get(host);
        if (cachedServerInfo != null) {
            connectionInfo.getProperties().put(ClickHouseClientOption.SERVER_VERSION.getKey(), cachedServerInfo.version.toString());
            connectionInfo.getProperties().put(ClickHouseClientOption.SERVER_TIME_ZONE.getKey(), cachedServerInfo.timeZone.getID());
        }

        ConnectionImpl clickHouseConnection;
        try {
            clickHouseConnection = RetryUtils.retry(() -> new ConnectionImpl(connectionInfo),
                                                    RetryableExceptions::isExceptionRetryable,
                                                    3,
                                                    Duration.ofMillis(100));
        } catch (Exception e) {
            throw new SQLException(e.getCause());
        }

        if (cachedServerInfo == null) {
            SERVER_INFO_CACHE.putIfAbsent(host, new ServerInfo(clickHouseConnection.getServerTimeZone(),
                                                               clickHouseConnection.getServerVersion()));
        }

        return clickHouseConnection;
    }

    private static class ConnectionImpl extends ClickHouseConnectionImpl {
        public ConnectionImpl(ClickHouseJdbcUrlParser.ConnectionInfo connectionInfo) throws SQLException {
            super(connectionInfo);
        }

        /**
         * Cache the table columns to reduce requests to ClickHouse
         */
        protected List<ClickHouseColumn> getTableColumns(String dbName,
                                                         String tableName,
                                                         String columns) throws SQLException {

            Map<String, List<ClickHouseColumn>> tableColumnCache = COLUMN_LIST_CACHE.computeIfAbsent(tableName, v -> new HashMap<>());

            List<ClickHouseColumn> tableColumns = tableColumnCache.get(columns);
            if (tableColumns == null) {
                synchronized (tableColumnCache) {
                    tableColumns = tableColumnCache.get(columns);
                    if (tableColumns != null) {
                        return tableColumns;
                    }

                    tableColumns = super.getTableColumns(dbName, tableName, columns);
                    tableColumnCache.put(columns, tableColumns);
                    return tableColumns;
                }
            }

            return tableColumns;
        }
    }
}
