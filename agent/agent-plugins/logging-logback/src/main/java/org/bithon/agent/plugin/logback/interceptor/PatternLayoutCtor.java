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

package org.bithon.agent.plugin.logback.interceptor;

import org.bithon.agent.bootstrap.aop.AbstractInterceptor;
import org.bithon.agent.bootstrap.aop.AopContext;
import org.bithon.agent.observability.tracing.context.ITraceSpan;
import org.bithon.agent.observability.tracing.context.TraceContextListener;
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

    private volatile TraceContextListener.IListener mdcUpdater = null;

    @Override
    public void onConstruct(AopContext aopContext) {
        if (mdcUpdater == null) {
            synchronized (this) {
                // double check
                if (mdcUpdater == null) {
                    mdcUpdater = new MdcUpdater();
                    TraceContextListener.getInstance().addListener(new MdcUpdater());
                }
            }
        }
    }

    private static class MdcUpdater implements TraceContextListener.IListener {
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
    }
}
