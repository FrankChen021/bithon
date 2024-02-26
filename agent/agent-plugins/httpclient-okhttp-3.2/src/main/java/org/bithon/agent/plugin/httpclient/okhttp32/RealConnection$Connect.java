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

package org.bithon.agent.plugin.httpclient.okhttp32;

import okhttp3.HttpUrl;
import okhttp3.Route;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.InterceptionDecision;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AroundInterceptor;
import org.bithon.agent.observability.tracing.context.ITraceSpan;
import org.bithon.agent.observability.tracing.context.TraceSpanFactory;
import org.bithon.component.commons.tracing.Tags;
import org.bithon.component.commons.utils.ReflectionUtils;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/10/28 16:50
 */
public class RealConnection$Connect extends AroundInterceptor {

    @Override
    public InterceptionDecision before(AopContext aopContext) {
        ITraceSpan span = TraceSpanFactory.newSpan("httpclient");
        if (span == null) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        String peer = null;
        Route route = (Route) ReflectionUtils.getFieldValue(aopContext.getTargetAs(), "route");
        if (route != null) {
            HttpUrl httpUrl = route.address().url();
            peer = httpUrl.port() == -1 ? httpUrl.host() : (httpUrl.host() + ":" + httpUrl.port());
        }
        aopContext.setSpan(span.tag(Tags.Http.CLIENT, "okhttp3")
                               // Since this span does not propagate the tracing context to next hop,
                               // it's not marked as SpanKind.CLIENT
                               .method(aopContext.getTargetClass().getName(), "connect")
                               .tag(Tags.Net.PEER, peer)
                               .start());
        return InterceptionDecision.CONTINUE;
    }

    @Override
    public void after(AopContext aopContext) {
        ITraceSpan span = aopContext.getSpan();
        span.tag(aopContext.getException()).finish();
    }
}
