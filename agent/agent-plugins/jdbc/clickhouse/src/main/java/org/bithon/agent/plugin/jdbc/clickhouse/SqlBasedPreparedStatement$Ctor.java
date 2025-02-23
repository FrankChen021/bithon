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

import com.clickhouse.client.ClickHouseRequest;
import com.clickhouse.jdbc.internal.ClickHouseConnectionImpl;
import com.clickhouse.jdbc.parser.ClickHouseSqlStatement;
import org.bithon.agent.instrumentation.aop.IBithonObject;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AfterInterceptor;
import org.bithon.agent.plugin.jdbc.common.StatementContext;

/**
 * {@link com.clickhouse.jdbc.internal.SqlBasedPreparedStatement#SqlBasedPreparedStatement(ClickHouseConnectionImpl, ClickHouseRequest, ClickHouseSqlStatement, int, int, int)}
 * <p>
 * Inject the {@link ClickHouseSqlStatement} so that {@link SqlBasedPreparedStatement$Execute} can access the statement
 *
 * @author frank.chen021@outlook.com
 * @date 2025/1/18 11:41
 */
public class SqlBasedPreparedStatement$Ctor extends AfterInterceptor {
    @Override
    public void after(AopContext aopContext) {
        IBithonObject preparedStatement = aopContext.getTargetAs();

        ClickHouseSqlStatement statement = aopContext.getArgAs(2);
        preparedStatement.setInjectedObject(new StatementContext(statement.getSQL()));
    }
}
