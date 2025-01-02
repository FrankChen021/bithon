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

package org.bithon.agent.plugin.jdbc.clickhouse;

import com.clickhouse.jdbc.internal.ClickHouseConnectionImpl;
import com.clickhouse.jdbc.internal.ClickHouseJdbcUrlParser;
import org.bithon.agent.instrumentation.aop.IBithonObject;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AfterInterceptor;
import org.bithon.agent.plugin.jdbc.common.ConnectionContext;

import java.util.Properties;

/**
 * {@link com.clickhouse.jdbc.internal.ClickHouseConnectionImpl#ClickHouseConnectionImpl(String)}
 * {@link com.clickhouse.jdbc.internal.ClickHouseConnectionImpl#ClickHouseConnectionImpl(ClickHouseJdbcUrlParser.ConnectionInfo)}
 * {@link com.clickhouse.jdbc.internal.ClickHouseConnectionImpl#ClickHouseConnectionImpl(String, Properties)}
 *
 * @author frank.chen021@outlook.com
 * @date 2024/12/30 16:17
 */
public class ClickHouseConnectionImpl$Ctor extends AfterInterceptor {

    /**
     * Inject ConnectionContext on the connection object which will be used by statement interceptors
     */
    @Override
    public void after(AopContext aopContext) throws Exception {
        if (aopContext.hasException()) {
            return;
        }

        ClickHouseConnectionImpl connection = aopContext.getTargetAs();
        ((IBithonObject) connection).setInjectedObject(new ConnectionContext(connection.getMetaData().getURL(),
                                                                             connection.getMetaData().getUserName(),
                                                                             "clickhouse"));
    }
}
