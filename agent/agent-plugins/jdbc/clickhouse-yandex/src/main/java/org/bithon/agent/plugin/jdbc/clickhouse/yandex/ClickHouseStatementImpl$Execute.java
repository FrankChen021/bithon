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


import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.plugin.jdbc.common.AbstractStatement$Execute;

/**
 * Hook on execute methods implemented in {@link org.h2.jdbc.JdbcStatement} as
 * {@link java.sql.Statement#execute(String)}
 * {@link java.sql.Statement#execute(String, int[])}
 * {@link java.sql.Statement#execute(String, String[])}
 * {@link java.sql.Statement#execute(String)}
 * <p>
 * {@link java.sql.Statement#executeQuery(String)}
 * <p>
 * {@link java.sql.Statement#executeUpdate(String)}
 * {@link java.sql.Statement#executeUpdate(String, int[])}
 * {@link java.sql.Statement#executeUpdate(String, String[])}
 * {@link java.sql.Statement#executeUpdate(String)}
 * <p>
 * {@link java.sql.Statement#executeLargeUpdate(String)}
 * {@link java.sql.Statement#executeLargeUpdate(String, int[])}
 * {@link java.sql.Statement#executeLargeUpdate(String, int)}
 * {@link java.sql.Statement#executeLargeUpdate(String, String[])}
 *
 * {@link ru.yandex.clickhouse.ClickHouseStatementImpl}
 * @author frankchen
 */
public class ClickHouseStatementImpl$Execute extends AbstractStatement$Execute {
    @Override
    protected String getStatement(AopContext aopContext) {
        return aopContext.getArgAs(0);
    }
}
