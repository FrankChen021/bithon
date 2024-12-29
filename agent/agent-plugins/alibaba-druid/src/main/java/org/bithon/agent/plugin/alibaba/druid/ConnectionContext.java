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

package org.bithon.agent.plugin.alibaba.druid;

import com.alibaba.druid.pool.DruidPooledConnection;
import org.bithon.agent.instrumentation.aop.IBithonObject;
import org.bithon.agent.observability.utils.MiscUtils;

/**
 * @author frank.chen021@outlook.com
 * @date 2024/12/29 21:58
 */
public class ConnectionContext {
    private final String connectionString;
    private final String username;

    public ConnectionContext(String connectionString, String username) {
        this.connectionString = MiscUtils.cleanupConnectionString(connectionString);
        this.username = username;
    }

    public String getConnectionString() {
        return connectionString;
    }

    public String getUsername() {
        return username;
    }

    public static ConnectionContext from(DruidPooledConnection connection) {
        return (ConnectionContext) ((IBithonObject) connection).getInjectedObject();
    }
}
