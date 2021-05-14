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

package com.sbss.bithon.agent.plugin.netty.interceptor;

import com.sbss.bithon.agent.bootstrap.aop.AbstractInterceptor;
import com.sbss.bithon.agent.bootstrap.aop.AopContext;
import com.sbss.bithon.agent.bootstrap.aop.InterceptionDecision;
import com.sbss.bithon.agent.core.tracing.context.SpanKind;
import com.sbss.bithon.agent.core.tracing.context.TraceContext;
import com.sbss.bithon.agent.core.tracing.context.TraceContextHolder;
import com.sbss.bithon.agent.core.tracing.context.TraceSpan;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.handler.codec.http.HttpRequest;

/**
 * interceptor of {@link org.jboss.netty.channel.Channels#write(Channel, Object)}
 *
 * @author frank.chen021@outlook.com
 * @date 2021/5/13 5:32 下午
 */
public class ChannelsWrite extends AbstractInterceptor {

    @Override
    public InterceptionDecision onMethodEnter(AopContext aopContext) throws Exception {
        if (!(aopContext.getArgs()[1] instanceof HttpRequest)) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        HttpRequest httpRequest = (HttpRequest) aopContext.getArgs()[1];

        TraceContext traceContext = TraceContextHolder.get();
        if (traceContext == null) {
            return InterceptionDecision.SKIP_LEAVE;
        }
        TraceSpan span = traceContext.currentSpan();
        if (span == null) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        aopContext.setUserContext(span.newChildSpan("nettyHttpClient")
                                      .method(aopContext.getMethod())
                                      .kind(SpanKind.CLIENT)
                                      .tag("uri", httpRequest.getUri())
                                      .start());

        //
        // propagate tracing after span creation
        //
        traceContext.propagate(httpRequest.headers(), (headersArgs, key, value) -> {
            headersArgs.set(key, value);
        });

        return super.onMethodEnter(aopContext);
    }

    @Override
    public void onMethodLeave(AopContext aopContext) {
        if (aopContext.hasException()) {
            return;
        }

        final TraceSpan span = (TraceSpan) aopContext.getUserContext();

        // unlink reference
        aopContext.setUserContext(null);

        ChannelFuture future = aopContext.castTargetAs();
        future.addListener(channelFuture -> {
            try {
                span.tag(channelFuture.getCause());
                span.finish();
            } catch (Exception ignored) {
            }
        });
    }
}
