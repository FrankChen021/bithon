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

import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AfterInterceptor;
import org.bithon.agent.observability.tracing.context.ITraceContext;
import org.bithon.agent.observability.tracing.context.TraceContextHolder;
import org.bithon.component.commons.tracing.Tags;

import java.util.Map;

/**
 * {@link org.apache.druid.sql.SqlLifecycle#initialize(String, Map)}
 *
 * @author frank.chen021@outlook.com
 * @date 4/1/22 6:37 PM
 */
public class SqlLifecycle$Initialize extends AfterInterceptor {

    @Override
    public void after(AopContext aopContext) {
        ITraceContext ctx = TraceContextHolder.current();
        if (ctx == null) {
            return;
        }

        String sql = aopContext.getArgAs(0);
        if (sql != null && !ctx.currentSpan().tags().containsKey("sql")) {
            ctx.currentSpan()
               .tag("druid.query_id", aopContext.getReturning())
               .tag(Tags.Database.STATEMENT, sql.toString());
        }
    }
}
