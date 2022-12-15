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

package org.bithon.agent.plugin.apache.ozone.interceptor;

import org.bithon.agent.bootstrap.aop.AbstractInterceptor;
import org.bithon.agent.bootstrap.aop.AopContext;
import org.bithon.agent.bootstrap.aop.InterceptionDecision;
import org.bithon.agent.core.tracing.context.ITraceSpan;
import org.bithon.agent.core.tracing.context.TraceSpanFactory;

/**
 * Tracing support to all methods in org.apache.hadoop.ozone.client.rpc.RpcClient
 *
 * @author Frank Chen
 * @date 14/12/22 10:37 pm
 */
public class RpcClient$All extends AbstractInterceptor {

    @Override
    public InterceptionDecision onMethodEnter(AopContext aopContext) throws Exception {
        ITraceSpan span = TraceSpanFactory.newSpan("ozone");
        if (span == null) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        aopContext.setUserContext(span.method(aopContext.getMethod()).start());

        return super.onMethodEnter(aopContext);
    }

    @Override
    public void onMethodLeave(AopContext aopContext) {
        ITraceSpan span = aopContext.castUserContextAs();
        span.tag(aopContext.getException()).finish();
    }
}
