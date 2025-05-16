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

package org.bithon.agent.plugin.jdbc.clickhouse.yandex;

import org.apache.http.impl.client.CloseableHttpClient;
import org.bithon.agent.instrumentation.aop.IBithonObject;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AfterInterceptor;
import org.bithon.agent.plugin.jdbc.common.StatementContext;
import ru.yandex.clickhouse.ClickHouseConnection;
import ru.yandex.clickhouse.settings.ClickHouseProperties;

import java.util.TimeZone;

/**
 * {@link ru.yandex.clickhouse.ClickHousePreparedStatementImpl#ClickHousePreparedStatementImpl(CloseableHttpClient, ClickHouseConnection, ClickHouseProperties, String, TimeZone, int)}
 *
 * @author frankchen
 */
public class ClickHousePreparedStatementImpl$Ctor extends AfterInterceptor {

    /**
     * Inject SQL so that {@link ClickHousePreparedStatementImpl$Execute} can use
     */
    @Override
    public void after(AopContext aopContext) {
        String sql = aopContext.getArgAs(3);

        IBithonObject preparedStatement = aopContext.getTargetAs();
        preparedStatement.setInjectedObject(new StatementContext(sql));
    }
}
