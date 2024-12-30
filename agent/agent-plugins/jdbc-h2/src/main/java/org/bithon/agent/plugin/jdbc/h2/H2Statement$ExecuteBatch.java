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

import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.observability.tracing.context.ITraceSpan;
import org.bithon.agent.plugin.jdbc.common.AbstractStatementExecute;
import org.bithon.component.commons.tracing.Tags;

/**
 * {@link org.postgresql.jdbc.PgStatement#executeBatch()}
 * {@link org.postgresql.jdbc.PgStatement#executeLargeBatch()}
 *
 * @author frank.chen021@outlook.com
 * @date 2024/12/29 20:15
 */
public class H2Statement$ExecuteBatch extends AbstractStatementExecute {

    @Override
    protected String getStatement(AopContext aopContext) {
        return null;
    }

    @Override
    protected void fillSpan(AopContext aopContext, ITraceSpan span) {
        if (aopContext.getReturning() != null) {
            int rows = 0;
            if (aopContext.getMethod().equals("executeBatch")) {
                rows = ((int[]) aopContext.getReturning()).length;
            } else {
                rows = ((long[]) aopContext.getReturning()).length;
            }
            span.tag(Tags.Database.PREFIX + "rows", rows);
        }
    }
}
