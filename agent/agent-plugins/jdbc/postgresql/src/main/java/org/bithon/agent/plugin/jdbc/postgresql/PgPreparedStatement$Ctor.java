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
import org.bithon.agent.plugin.jdbc.common.SqlTypeParser;
import org.bithon.agent.plugin.jdbc.common.StatementContext;
import org.postgresql.core.CachedQuery;
import org.postgresql.core.SqlCommandType;
import org.postgresql.jdbc.PgConnection;

/**
 * Hook on
 * {@link org.postgresql.jdbc.PgPreparedStatement#PgPreparedStatement(PgConnection, CachedQuery, int, int, int)}
 * to hold the SQL being executed
 *
 * @author frankchen
 */
public class PgPreparedStatement$Ctor extends AfterInterceptor {

    /**
     * Inject the query object for further use
     */
    @Override
    public void after(AopContext aopContext) {
        CachedQuery query = aopContext.getArgAs(1);

        String sqlType = "";
        String sql = query.query.toString();
        if (query.query.getSqlCommand() != null && query.query.getSqlCommand().getType() != SqlCommandType.BLANK) {
            sqlType = query.query.getSqlCommand().getType().name();
        } else {
            sqlType = SqlTypeParser.parse(sql);
        }
        IBithonObject preparedStatement = aopContext.getTargetAs();
        preparedStatement.setInjectedObject(new StatementContext(sql, sqlType));
    }
}
