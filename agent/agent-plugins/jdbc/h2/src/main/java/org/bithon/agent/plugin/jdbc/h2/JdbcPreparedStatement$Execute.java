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

package org.bithon.agent.plugin.jdbc.h2;

import org.bithon.agent.instrumentation.aop.IBithonObject;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.plugin.jdbc.common.AbstractStatement$Execute;
import org.bithon.agent.plugin.jdbc.common.StatementContext;

/**
 * {@link org.h2.jdbc.JdbcPreparedStatement#execute()}
 * {@link org.h2.jdbc.JdbcPreparedStatement#executeQuery()}
 * {@link org.h2.jdbc.JdbcPreparedStatement#executeUpdate()}
 *
 * @author frankchen
 */
public class JdbcPreparedStatement$Execute extends AbstractStatement$Execute {

    /**
     * The executing statement is injected by {@link JdbcPreparedStatement$Ctor}
     */
    @Override
    protected StatementContext getStatementContext(AopContext aopContext) {
        IBithonObject preparedStatement = aopContext.getTargetAs();
        return (StatementContext) preparedStatement.getInjectedObject();
    }
}
