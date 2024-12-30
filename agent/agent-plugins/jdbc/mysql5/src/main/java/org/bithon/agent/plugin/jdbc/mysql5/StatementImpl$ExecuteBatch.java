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

package org.bithon.agent.plugin.jdbc.mysql5;

import com.mysql.jdbc.MySQLConnection;
import org.bithon.agent.plugin.jdbc.common.AbstractStatement$ExecuteBatch;
import org.bithon.agent.plugin.jdbc.common.ConnectionContext;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * {@link com.mysql.jdbc.StatementImpl#executeBatch()}
 * {@link com.mysql.jdbc.StatementImpl#executeLargeBatch()}
 *
 * @author frank.chen021@outlook.com
 * @date 2024/12/29 20:15
 */
public class StatementImpl$ExecuteBatch extends AbstractStatement$ExecuteBatch {
    @Override
    protected ConnectionContext getConnectionContext(Connection connection) throws SQLException {
        return new ConnectionContext(connection.getMetaData().getURL(),
                                     // DON'T call getUser on getMetaData which will issue a query to the server,
                                     // which result in recursive call to this method
                                     ((MySQLConnection) connection).getUser(),
                                     "mysql");
    }
}
