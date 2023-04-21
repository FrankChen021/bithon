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

import org.apache.druid.server.QueryLifecycle;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AfterInterceptor;
import org.bithon.agent.observability.tracing.context.ITraceContext;
import org.bithon.agent.observability.tracing.context.TraceContextHolder;
import org.bithon.component.commons.logging.LoggerFactory;
import org.bithon.shaded.com.fasterxml.jackson.core.JsonProcessingException;
import org.bithon.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.bithon.shaded.com.fasterxml.jackson.databind.SerializationFeature;
import org.bithon.shaded.com.fasterxml.jackson.datatype.joda.JodaModule;

/**
 * @author frank.chen021@outlook.com
 * @date 4/1/22 6:38 PM
 */
public class QueryLifecycle$Initialize extends AfterInterceptor {

    private final ObjectMapper om;

    public QueryLifecycle$Initialize() {
        this.om = new ObjectMapper().configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                                    .configure(SerializationFeature.INDENT_OUTPUT, true);
        /*
          NOTE
          JodaModule is defined in com.fasterxml.jackson.datatype:jackson-datatype-joda and is shaded in shaded-jackson module
          but the org.joda.time.* classes in that module is excluded from the shaded module
          This is because that package is not shaded, we want to use these class loaded in user's class loader to do serialization.
          These classes are used by the Query object below.

          This is not the best way because we can't use the shaded jackson to serialize/deserialize shaded joda objects anymore.
         */
        this.om.registerModule(new JodaModule());
    }

    @Override
    public void after(AopContext aopContext) {
        ITraceContext ctx = TraceContextHolder.current();
        if (ctx != null) {
            QueryLifecycle lifecycle = aopContext.getTargetAs();
            if (!ctx.currentSpan().tags().containsKey("query")) {
                try {
                    ctx.currentSpan()
                       .tag("query_id", lifecycle.getQuery().getId())
                       .tag("query", om.writeValueAsString(lifecycle.getQuery()));
                } catch (JsonProcessingException e) {
                    LoggerFactory.getLogger(QueryLifecycle$Initialize.class).error("Unable to serialize query object: {}", e.getMessage());
                }
            }
        }
    }
}
