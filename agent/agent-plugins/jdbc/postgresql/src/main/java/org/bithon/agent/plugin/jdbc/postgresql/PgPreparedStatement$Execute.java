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
import org.bithon.agent.plugin.jdbc.common.AbstractStatement$Execute;
import org.bithon.agent.plugin.jdbc.common.ConnectionContext;
import org.bithon.agent.plugin.jdbc.common.StatementContext;

import java.sql.Connection;

/**
 * {@link org.postgresql.jdbc.PgPreparedStatement#execute()}
 * {@link org.postgresql.jdbc.PgPreparedStatement#executeQuery()}
 * {@link org.postgresql.jdbc.PgPreparedStatement#executeUpdate()}
 *
 * @author frankchen
 */
public class PgPreparedStatement$Execute extends AbstractStatement$Execute {
    /**
     * Get the connection context which is injected by {@link PgConnection$Ctor}
     */
    @Override
    protected ConnectionContext getConnectionContext(Connection connection) {
        IBithonObject instrumentedConnection = (IBithonObject) connection;
        return (ConnectionContext) instrumentedConnection.getInjectedObject();
    }

    @Override
    protected StatementContext getStatement(AopContext aopContext) {
        IBithonObject preparedStatement = aopContext.getTargetAs();
        return (StatementContext) preparedStatement.getInjectedObject();
    }
}
