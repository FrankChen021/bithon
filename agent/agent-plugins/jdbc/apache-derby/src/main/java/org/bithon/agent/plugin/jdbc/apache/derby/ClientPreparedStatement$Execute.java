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

package org.bithon.agent.plugin.jdbc.apache.derby;

import org.bithon.agent.instrumentation.aop.IBithonObject;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.plugin.jdbc.common.AbstractStatement$Execute;
import org.bithon.agent.plugin.jdbc.common.StatementContext;

/**
 * {@link org.apache.derby.client.am.ClientPreparedStatement#execute()}
 * {@link org.apache.derby.client.am.ClientPreparedStatement#executeQuery()}
 * {@link org.apache.derby.client.am.ClientPreparedStatement#executeUpdate()}
 *
 * @author frankchen
 */
public class ClientPreparedStatement$Execute extends AbstractStatement$Execute {

    /**
     * The executing statement is injected by {@link ClientPreparedStatement$Ctor}
     */
    @Override
    protected StatementContext getStatementContext(AopContext aopContext) {
        IBithonObject preparedStatement = aopContext.getTargetAs();
        return (StatementContext) preparedStatement.getInjectedObject();
    }
}
