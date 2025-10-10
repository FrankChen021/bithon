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

package org.bithon.agent.plugin.jdbc.presto;

import io.prestosql.jdbc.PrestoPreparedStatement;
import org.bithon.agent.instrumentation.aop.IBithonObject;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.observability.utils.MiscUtils;
import org.bithon.agent.plugin.jdbc.common.AbstractStatement$ExecuteBatch;
import org.bithon.agent.plugin.jdbc.common.ConnectionContext;
import org.bithon.agent.plugin.jdbc.common.StatementContext;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * {@link io.prestosql.jdbc.PrestoStatement#executeBatch()}
 * {@link io.prestosql.jdbc.PrestoStatement#executeLargeBatch()}
 *
 * @author frank.chen021@outlook.com
 * @date 2025/10/02
 */
public class PrestoStatement$ExecuteBatch extends AbstractStatement$ExecuteBatch {
    @Override
    protected ConnectionContext getConnectionContext(Connection connection) throws SQLException {
        return new ConnectionContext(
            MiscUtils.cleanupConnectionString(connection.getMetaData().getURL()),
            connection.getMetaData().getUserName(),
            "presto");
    }

    @Override
    protected StatementContext getStatementContext(AopContext aopContext) {
        Object statement = aopContext.getTarget();
        if (statement instanceof PrestoPreparedStatement) {
            // The context is injected by PrestoPreparedStatement$Ctor
            return (StatementContext) ((IBithonObject) statement).getInjectedObject();
        }

        // Fallback to EMPTY
        return StatementContext.EMPTY;
    }
}

