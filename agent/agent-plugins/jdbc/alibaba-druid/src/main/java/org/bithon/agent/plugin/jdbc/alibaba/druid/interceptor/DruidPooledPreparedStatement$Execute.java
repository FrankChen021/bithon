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

package org.bithon.agent.plugin.jdbc.alibaba.druid.interceptor;

import com.alibaba.druid.pool.DruidPooledPreparedStatement;
import com.alibaba.druid.pool.DruidPooledStatement;
import org.bithon.agent.instrumentation.aop.IBithonObject;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.plugin.jdbc.common.AbstractStatement$Execute;
import org.bithon.agent.plugin.jdbc.common.StatementContext;

import java.sql.Statement;

/**
 * {@link com.alibaba.druid.pool.DruidPooledPreparedStatement#execute()}
 * {@link com.alibaba.druid.pool.DruidPooledPreparedStatement#executeQuery()}
 * {@link com.alibaba.druid.pool.DruidPooledPreparedStatement#executeUpdate()}
 *
 * @author frankchen
 * @date 2022-07-27
 */
public class DruidPooledPreparedStatement$Execute extends AbstractStatement$Execute {

    @Override
    protected boolean shouldRecordMetrics(Statement statement) {
        DruidPooledStatement druidStatement = (DruidPooledStatement) statement;
        boolean isInstrumented = druidStatement.getStatement() instanceof IBithonObject;

        // Only records metrics at Druid level when underlying statement is not instrumented
        return !isInstrumented;
    }

    @Override
    protected StatementContext getStatementContext(AopContext aopContext) {
        return new StatementContext(((DruidPooledPreparedStatement) aopContext.getTarget()).getSql());
    }
}
