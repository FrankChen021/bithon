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

package org.bithon.agent.plugin.jdbc.postgresql;

import org.bithon.agent.instrumentation.aop.IBithonObject;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AfterInterceptor;
import org.bithon.agent.observability.utils.MiscUtils;
import org.bithon.agent.plugin.jdbc.common.ConnectionContext;
import org.bithon.component.commons.utils.StringUtils;
import org.postgresql.core.QueryExecutor;
import org.postgresql.jdbc.PgConnection;
import org.postgresql.util.HostSpec;

import java.util.Properties;

/**
 * Hook on
 * {@link org.postgresql.jdbc.PgConnection#PgConnection(HostSpec[], Properties, String)}
 * to hold the cleaned connection string
 *
 * @author frank.chen021@outlook.com
 * @date 2024/12/30 09:51
 */
public class PgConnection$Ctor extends AfterInterceptor {
    @Override
    public void after(AopContext aopContext) throws Exception {
        PgConnection connection = aopContext.getTargetAs();

        String connectionString;

        // Get the established local endpoint because the configured connection string might be a list of endpoints
        QueryExecutor queryExecutor = connection.getQueryExecutor();
        if (queryExecutor != null) {
            connectionString = StringUtils.format("jdbc:postgresql://%s:%d",
                                                  queryExecutor.getHostSpec().getHost(),
                                                  queryExecutor.getHostSpec().getPort());

            if (StringUtils.isEmpty(queryExecutor.getDatabase())) {
                connectionString += "/" + queryExecutor.getDatabase();
            }
        } else {
            // Fallback
            connectionString = MiscUtils.cleanupConnectionString(connection.getURL());
        }

        ((IBithonObject) connection).setInjectedObject(
            new ConnectionContext(connectionString,
                                  connection.getUserName(),
                                  "postgresql")
        );
    }
}
