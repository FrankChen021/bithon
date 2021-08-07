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

package com.sbss.bithon.agent.plugin.logback.interceptor;

import com.sbss.bithon.agent.bootstrap.aop.AbstractInterceptor;
import com.sbss.bithon.agent.bootstrap.aop.AopContext;
import com.sbss.bithon.agent.core.tracing.context.ITraceSpan;
import com.sbss.bithon.agent.core.tracing.context.TraceContextListener;
import org.slf4j.MDC;

/**
 * {@link ch.qos.logback.core.pattern.PatternLayoutBase#PatternLayoutBase()}
 * <p>
 * add txId:spanId pattern to the user's pattern
 * <p>
 * [bTxId:xxx, bSpanId:xxx]
 *
 * @author frank.chen021@outlook.com
 * @date 2021/8/7 9:51 下午
 */
public class PatternLayoutCtor extends AbstractInterceptor {

    @Override
    public void onConstruct(AopContext aopContext) throws Exception {
        TraceContextListener.getInstance().addListener(new TraceContextListener.IListener() {
            @Override
            public void onSpanStarted(ITraceSpan span) {
                MDC.put("bTxId", span.traceId());
                MDC.put("bSpanId", span.spanId());
            }

            @Override
            public void onSpanFinished(ITraceSpan span) {
                if (span.context().currentSpan() == null) {
                    MDC.remove("bTxId");
                    MDC.remove("bSpanId");
                } else {
                    MDC.put("bTxId", span.traceId());
                    MDC.put("bSpanId", span.spanId());
                }
            }
        });
    }
}
