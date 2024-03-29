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

package org.bithon.server.commons.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/22
 */
public class DbUtils {

    /**
     * connection string template:
     * jdbc:h2:mem:007af5e4-ee6e-4af5-a515-f961f0fd02a1;a=b
     * jdbc:mysql://localhost:3306/bithon?useUnicode=true&amp;useSSL=false&amp;autoReconnect=TRUE
     * jdbc:netezza://main:5490/sales;user=admin;password=password;loglevel=2
     */
    public static ConnectionString tryParseConnectionString(String connectionString) {
        try {
            return parseConnectionString(connectionString);
        } catch (Exception ignored) {
            return null;
        }
    }

    public static ConnectionString parseConnectionString(String connectionString) {
        if (StringUtils.isEmpty(connectionString)) {
            throw new RuntimeException(String.format(Locale.ENGLISH,
                                                     "Connection String of SqlMetricMessage is blank: [%s]",
                                                     connectionString));
        }

        if (!connectionString.startsWith("jdbc:")) {
            throw new RuntimeException(String.format(Locale.ENGLISH, "Unknown format of Connection String: [%s]", connectionString));
        }
        connectionString = connectionString.substring(5);

        try {
            URI uri = new URI(connectionString);
            switch (uri.getScheme()) {
                case "h2":
                    return new ConnectionString("localhost",
                                                uri.getSchemeSpecificPart(),
                                                "h2");

                case "mysql":
                    return new ConnectionString(uri.getHost() + ":" + uri.getPort(),
                                                uri.getPath().substring(1),
                                                "mysql");
                case "ch":
                case "clickhouse":
                    if (uri.getPath() == null) {
                        //
                        // ClickHouse JDBC Driver sometimes turns jdbc:clickhouse:// into jdbc:clickhouse:http://
                        // So we need to drop the jdbc:clickhouse: if necessary
                        //
                        uri = new URI(connectionString.substring(uri.getScheme().length() + 1));
                    }
                    return new ConnectionString(uri.getHost() + ":" + uri.getPort(),
                                                uri.getPath().substring(1),
                                                "clickhouse");
                default:
                    throw new RuntimeException(String.format(Locale.ENGLISH, "Unknown schema of Connection String: [%s]",
                                                             connectionString));
            }
        } catch (URISyntaxException e) {
            throw new RuntimeException(String.format(Locale.ENGLISH, "Invalid format of Connection String: [%s]", connectionString));
        }
    }

    @Getter
    @AllArgsConstructor
    public static class ConnectionString {
        private final String hostAndPort;
        private final String database;
        private final String dbType;
    }
}
