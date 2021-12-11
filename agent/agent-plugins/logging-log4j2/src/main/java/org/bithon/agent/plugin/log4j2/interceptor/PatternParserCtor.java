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

package org.bithon.agent.plugin.log4j2.interceptor;

import org.apache.logging.log4j.ThreadContext;
import org.bithon.agent.bootstrap.aop.AbstractInterceptor;
import org.bithon.agent.bootstrap.aop.AopContext;
import org.bithon.agent.core.tracing.context.ITraceSpan;
import org.bithon.agent.core.tracing.context.TraceContextListener;

import java.util.List;

/**
 * {@link org.apache.logging.log4j.core.pattern.PatternParser#parse(String, List, List, boolean, boolean, boolean)}
 * <p>
 * automatically inject trace id into user's log pattern
 *
 * @author frank.chen021@outlook.com
 * @date 2021/8/7 9:56 下午
 */
public class PatternParserCtor extends AbstractInterceptor {

    @Override
    public void onConstruct(AopContext aopContext) {
        TraceContextListener.getInstance().addListener(new TraceContextListener.IListener() {
            @Override
            public void onSpanStarted(ITraceSpan span) {
                ThreadContext.put("bTxId", span.traceId());
                ThreadContext.put("bSpanId", span.spanId());
            }

            @Override
            public void onSpanFinished(ITraceSpan span) {
                if (span.context().currentSpan() == null) {
                    ThreadContext.remove("bTxId");
                    ThreadContext.remove("bSpanId");
                } else {
                    ThreadContext.put("bTxId", span.traceId());
                    ThreadContext.put("bSpanId", span.spanId());
                }
            }
        });
    }
}
