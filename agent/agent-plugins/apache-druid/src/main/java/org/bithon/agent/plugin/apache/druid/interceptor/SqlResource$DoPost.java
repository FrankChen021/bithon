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

package org.bithon.agent.plugin.apache.druid.interceptor;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.druid.sql.http.SqlQuery;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.BeforeInterceptor;
import org.bithon.agent.observability.tracing.context.ITraceContext;
import org.bithon.agent.observability.tracing.context.TraceContextHolder;
import org.bithon.component.commons.tracing.Tags;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.UUID;

/**
 * {@link org.apache.druid.sql.http.SqlResource#doPost(SqlQuery, HttpServletRequest)}
 *
 * @author frank.chen021@outlook.com
 * @date 2024/11/5 21:39
 */
public class SqlResource$DoPost extends BeforeInterceptor {

    /**
     * We don't create a new span but attach relavenat information to the current span.
     * This is because the jersey plugin already created a span for the incoming request.
     */
    @Override
    public void before(AopContext aopContext) {
        ITraceContext ctx = TraceContextHolder.current();
        if (ctx == null) {
            return;
        }

        SqlQuery sqlQuery = aopContext.getArgAs(0);
        Object sqlQueryId = sqlQuery.getContext().get("sqlQueryId");
        if (sqlQueryId == null) {
            try {
                sqlQueryId = UUID.randomUUID().toString();

                ObjectNode newQuery = ObjectMapperInstance.toTree(sqlQuery);
                ObjectNode context = (ObjectNode) newQuery.get("context");
                if (context == null) {
                    context = newQuery.putObject("context");
                }
                context.put("sqlQueryId", (String) sqlQueryId);
                sqlQuery = ObjectMapperInstance.fromTree(newQuery, SqlQuery.class);

                // Update the input argument
                aopContext.getArgs()[0] = sqlQuery;
            } catch (IOException ignored) {
                // Ignore the exception
                sqlQueryId = "";
            }
        }

        ctx.currentSpan()
           .tag("druid.sql_query_id", sqlQueryId.toString())
           .tag(Tags.Database.STATEMENT, sqlQuery.getQuery());
    }
}
