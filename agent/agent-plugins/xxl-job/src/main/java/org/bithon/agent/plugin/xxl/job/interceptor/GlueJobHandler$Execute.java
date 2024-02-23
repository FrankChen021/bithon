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

package org.bithon.agent.plugin.xxl.job.interceptor;

import com.xxl.job.core.handler.IJobHandler;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.InterceptionDecision;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AroundInterceptor;
import org.bithon.agent.observability.tracing.context.ITraceSpan;
import org.bithon.agent.observability.tracing.context.TraceSpanFactory;
import org.bithon.component.commons.utils.ReflectionUtils;

/**
 * {@link com.xxl.job.core.handler.impl.GlueJobHandler#execute()}
 *
 * @author Frank Chen
 * @date 23/2/24 1:09 pm
 */
public class GlueJobHandler$Execute extends AroundInterceptor {

    @Override
    public InterceptionDecision before(AopContext aopContext) {
        ITraceSpan span = TraceSpanFactory.newSpan("glue-job");
        if (span == null) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        IJobHandler job = (IJobHandler) ReflectionUtils.getFieldValue(aopContext.getTarget(), "jobHandler");
        aopContext.setUserContext(span.method(job.getClass(), "execute")
                                      .start());

        return InterceptionDecision.CONTINUE;
    }

    @Override
    public void after(AopContext aopContext) {
        ITraceSpan span = aopContext.getUserContextAs();
        span.tag(aopContext.getException())
            .finish();
    }
}
