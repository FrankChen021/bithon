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

package org.bithon.agent.plugin.jdbc.druid.interceptor;

import org.bithon.agent.bootstrap.aop.AbstractInterceptor;
import org.bithon.agent.bootstrap.aop.AopContext;
import org.bithon.agent.bootstrap.aop.InterceptionDecision;
import org.bithon.agent.core.tracing.context.ITraceSpan;
import org.bithon.agent.core.tracing.context.SpanKind;
import org.bithon.agent.core.tracing.context.Tags;
import org.bithon.agent.core.tracing.context.TraceSpanFactory;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

/**
 * @author frankchen
 */
public class DruidTraceHandler extends AbstractInterceptor {
    private static final Logger log = LoggerFactory.getLogger(DruidTraceHandler.class);

    @Override
    public InterceptionDecision onMethodEnter(AopContext aopContext) {
        ITraceSpan span = TraceSpanFactory.newSpan("alibaba-druid");
        if (span == null) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        // create a span and save it in user-context
        aopContext.setUserContext(span.method(aopContext.getMethod())
                                      .kind(SpanKind.CLIENT)
                                      .tag(Tags.TARGET_TYPE, Tags.TargetType.Database.name())
                                      //TODO:
                                      //.tag("db", )
                                      .start());

        return InterceptionDecision.CONTINUE;
    }

    @Override
    public void onMethodLeave(AopContext aopContext) {
        ITraceSpan thisSpan = aopContext.castUserContextAs();
        if (thisSpan == null) {
            return;
        }

        try {
            if (aopContext.hasException()) {
                thisSpan.tag("exception", aopContext.getException().getClass().getSimpleName());
            }
            if (aopContext.getArgs() != null && aopContext.getArgs().length > 0) {
                thisSpan.tag("sql", aopContext.getArgs()[0].toString());
            }
        } finally {
            try {
                thisSpan.finish();
            } catch (Exception e) {
                log.warn("error to finish span", e);
            }
        }
    }
}
