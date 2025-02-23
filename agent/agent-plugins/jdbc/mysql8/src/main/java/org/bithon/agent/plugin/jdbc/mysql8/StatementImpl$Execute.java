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

package org.bithon.agent.plugin.jdbc.mysql8;


import com.mysql.cj.MysqlConnection;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.plugin.jdbc.common.AbstractStatement$Execute;
import org.bithon.agent.plugin.jdbc.common.ConnectionContext;
import org.bithon.agent.plugin.jdbc.common.StatementContext;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author frankchen
 */
public class StatementImpl$Execute extends AbstractStatement$Execute {

    @Override
    protected StatementContext getStatementContext(AopContext aopContext) {
        return new StatementContext(aopContext.getArgAs(0));
    }

    @Override
    protected ConnectionContext getConnectionContext(Connection connection) throws SQLException {
        return new ConnectionContext(connection.getMetaData().getURL(),
                                     // DON'T call getUser on getMetaData which will issue a query to the server
                                     ((MysqlConnection) connection).getUser(),
                                     "mysql");
    }
}
