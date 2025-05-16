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

package org.bithon.agent.plugin.httpserver.jetty12.interceptor;


import org.bithon.agent.instrumentation.aop.IBithonObject;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.InterceptionDecision;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AroundInterceptor;
import org.bithon.agent.observability.tracing.context.TraceContextHolder;
import org.bithon.agent.plugin.httpserver.jetty12.context.RequestContext;

/**
 * {@link org.eclipse.jetty.server.internal.HttpChannelState.HandlerInvoker#run()}
 *
 * @author frank.chen021@outlook.com
 * @date 17/3/25 12:08 am
 */
public class HandlerInvoker$Run extends AroundInterceptor {
    @Override
    public InterceptionDecision before(AopContext aopContext) throws Exception {
        IBithonObject bithonObject = aopContext.getTargetAs();
        RequestContext requestContext = (RequestContext) bithonObject.getInjectedObject();
        if (requestContext == null) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        requestContext.getTraceContext()
                      .currentSpan()
                      .method(aopContext.getTargetClass(), aopContext.getMethod());
        TraceContextHolder.attach(requestContext.getTraceContext());
        return super.before(aopContext);
    }

    @Override
    public void after(AopContext aopContext) {
        // The TraceSpan is closed in HttpChannelState$CompleteStream
        TraceContextHolder.detach();
    }
}
