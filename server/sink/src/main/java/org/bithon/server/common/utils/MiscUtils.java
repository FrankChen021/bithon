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
import org.bithon.server.meta.EndPointType;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/22
 */
public class MiscUtils {

    public static ConnectionString parseConnectionString(String connectionString) {
        if (StringUtils.isEmpty(connectionString)) {
            throw new RuntimeException(String.format(Locale.ENGLISH,
                                                     "Connection String of SqlMetricMessage is blank: [%s]",
                                                     connectionString));
        }

        if (!connectionString.startsWith("jdbc:")) {
            throw new RuntimeException(String.format(Locale.ENGLISH, "Unknown format of Connection String: [%s]", connectionString));
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
                case "clickhouse":
                    return new ConnectionString(uri.getHost() + ":" + uri.getPort(),
                                                uri.getPath().substring(1),
                                                EndPointType.DB_CLICKHOUSE);
                default:
                    throw new RuntimeException(String.format(Locale.ENGLISH, "Unknown schema of Connection String: [%s]",
                                                             connectionString));
            }
        } catch (URISyntaxException e) {
            throw new RuntimeException(String.format(Locale.ENGLISH, "Invalid format of Connection String: [%s]", connectionString));
        }
    }

    public static Map<String, String> parseURLParameters(String uriText) {
        if (uriText == null) {
            return Collections.emptyMap();
        }

        URI uri = null;
        try {
            uri = new URI(uriText);
        } catch (URISyntaxException ignored) {
            return Collections.emptyMap();
        }

        String query = uri.getQuery();
        if (!StringUtils.hasText(query)) {
            return Collections.emptyMap();
        }

        Map<String, String> variables = new TreeMap<>();
        int fromIndex = 0;
        int toIndex = 0;
        while (toIndex != -1) {
            String name;
            String value;
            toIndex = query.indexOf('=', fromIndex);
            if (toIndex - fromIndex > 1) {
                name = query.substring(fromIndex, toIndex);
                fromIndex = toIndex + 1;
                toIndex = query.indexOf('&', fromIndex);
                if (toIndex == -1) {
                    value = query.substring(fromIndex);
                } else {
                    value = query.substring(fromIndex, toIndex);
                }
                variables.put(name, value);
                fromIndex = toIndex + 1;
            } else {
                fromIndex = query.indexOf('&', toIndex) + 1;
            }
        }
        return variables;
    }

    @Getter
    @AllArgsConstructor
    public static class ConnectionString {
        private final String hostAndPort;
        private final String database;
        private final EndPointType endPointType;
    }
}
