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

package org.bithon.server.common.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bithon.component.db.dao.EndPointType;
import org.jooq.tools.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/22
 */
public class MiscUtils {

    @Getter
    @AllArgsConstructor
    public static class ConnectionString {
        private final String hostAndPort;
        private final String database;
        private final EndPointType endPointType;
    }

    public static ConnectionString parseConnectionString(String connectionString) {
        if (StringUtils.isBlank(connectionString)) {
            throw new RuntimeException(String.format("Connection String of SqlMetricMessage is blank: [%s]",
                                                     connectionString));
        }

        if (!connectionString.startsWith("jdbc:")) {
            throw new RuntimeException(String.format("Unknown format of Connection String: [%s]", connectionString));
        }

        try {
            URI uri = new URI(connectionString.substring(5));
            switch (uri.getScheme()) {
                case "h2":
                    return new ConnectionString("localhost", uri.getSchemeSpecificPart(), EndPointType.DB_H2);
                case "mysql":
                    return new ConnectionString(uri.getHost() + ":" + uri.getPort(),
                                                uri.getPath().substring(1),
                                                EndPointType.DB_MYSQL);
                default:
                    throw new RuntimeException(String.format("Unknown schema of Connection String: [%s]",
                                                             connectionString));
            }
        } catch (URISyntaxException e) {
            throw new RuntimeException(String.format("Invalid format of Connection String: [%s]", connectionString));
        }
    }
}
