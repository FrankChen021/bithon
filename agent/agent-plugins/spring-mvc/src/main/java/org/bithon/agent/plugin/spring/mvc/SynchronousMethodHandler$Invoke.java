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

package org.bithon.agent.plugin.spring.mvc;


import feign.MethodMetadata;
import org.bithon.agent.bootstrap.aop.AbstractInterceptor;
import org.bithon.agent.bootstrap.aop.AopContext;
import org.bithon.agent.bootstrap.aop.IBithonObject;
import org.bithon.agent.bootstrap.aop.InterceptionDecision;
import org.bithon.agent.core.tracing.context.ITraceSpan;
import org.bithon.agent.core.tracing.context.SpanKind;
import org.bithon.agent.core.tracing.context.TraceSpanFactory;

/**
 * {@link feign.SynchronousMethodHandler#invoke(Object[])}
 *
 * @author frankchen
 * @date 2021-02-16 14:41
 */
public class SynchronousMethodHandler$Invoke extends AbstractInterceptor {

    @Override
    public InterceptionDecision onMethodEnter(AopContext aopContext) {
        if (!(aopContext.getTarget() instanceof IBithonObject)) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        IBithonObject methodHandler = aopContext.castTargetAs();
        MethodMetadata metadata = (MethodMetadata) methodHandler.getInjectedObject();
        if (metadata == null) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        ITraceSpan span = TraceSpanFactory.newSpan("feignClient");
        if (span == null) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        aopContext.setUserContext(span.kind(SpanKind.CLIENT)
                                      .method(metadata.method())
                                      .start());

        return InterceptionDecision.CONTINUE;
    }

    @Override
    public void onMethodLeave(AopContext aopContext) {
        ITraceSpan span = aopContext.castUserContextAs();

        span.tag(aopContext.getException())
            .finish();
    }
}
