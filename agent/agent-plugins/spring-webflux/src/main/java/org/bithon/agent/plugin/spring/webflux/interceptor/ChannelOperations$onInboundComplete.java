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

package org.bithon.agent.plugin.spring.webflux.interceptor;

import org.bithon.agent.bootstrap.aop.AbstractInterceptor;
import org.bithon.agent.bootstrap.aop.AopContext;
import org.bithon.agent.bootstrap.aop.InterceptionDecision;
import org.bithon.agent.core.tracing.context.TraceContextHolder;
import org.bithon.agent.plugin.spring.webflux.context.HttpServerContext;
import reactor.netty.channel.ChannelOperations;

/**
 *
 * intercepts the {@link ChannelOperations#onInboundComplete()},
 *  which is called by {@link reactor.netty.http.server.HttpServerOperations#onInboundNext},
 *  which is called by Flux#subscribe
 *
 * @author Frank Chen
 * @date 3/1/22 5:10 PM
 */
public class ChannelOperations$onInboundComplete extends AbstractInterceptor {
    @Override
    public InterceptionDecision onMethodEnter(AopContext aopContext) {
        if (!"HttpServerOperations".equals(aopContext.getTarget().getClass().getSimpleName())) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        HttpServerContext context = aopContext.castInjectedOnTargetAs();
        if (context.getTraceContext() != null) {
            TraceContextHolder.set(context.getTraceContext());
            return InterceptionDecision.CONTINUE;
        }

        return InterceptionDecision.SKIP_LEAVE;
    }

    @Override
    public void onMethodLeave(AopContext aopContext) {
        TraceContextHolder.remove();
    }
}
