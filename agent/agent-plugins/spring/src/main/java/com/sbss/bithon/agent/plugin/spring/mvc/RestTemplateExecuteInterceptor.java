/*
 *    Copyright 2020 bithon.cn
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

package com.sbss.bithon.agent.plugin.spring.mvc;

import com.sbss.bithon.agent.bootstrap.aop.AbstractInterceptor;
import com.sbss.bithon.agent.bootstrap.aop.AopContext;
import com.sbss.bithon.agent.bootstrap.aop.InterceptionDecision;
import com.sbss.bithon.agent.core.tracing.context.ITraceContext;
import com.sbss.bithon.agent.core.tracing.context.ITraceSpan;
import com.sbss.bithon.agent.core.tracing.context.SpanKind;
import com.sbss.bithon.agent.core.tracing.context.TraceContextHolder;

import java.net.URI;

/**
 * @author frankchen
 * @date 2021-02-16 14:36
 */
public class RestTemplateExecuteInterceptor extends AbstractInterceptor {
    @Override
    public InterceptionDecision onMethodEnter(AopContext aopContext) {
        ITraceContext traceContext = TraceContextHolder.get();
        if (traceContext == null) {
            return InterceptionDecision.SKIP_LEAVE;
        }
        ITraceSpan span = traceContext.currentSpan();
        if (span == null) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        String uri = null;
        Object obj = aopContext.getArgs()[0];
        if (obj instanceof String) {
            uri = (String) obj;
        } else if (obj instanceof URI) {
            uri = obj.toString();
        }

        aopContext.setUserContext(span.newChildSpan("restTemplate")
                                      .method(aopContext.getMethod())
                                      .kind(SpanKind.CLIENT)
                                      .tag("uri", uri)
                                      .start());

        return InterceptionDecision.CONTINUE;
    }


    @Override
    public void onMethodLeave(AopContext aopContext) {
        ITraceSpan span = aopContext.castUserContextAs();
        if (span == null) {
            return;
        }
        span.finish();
    }
}
