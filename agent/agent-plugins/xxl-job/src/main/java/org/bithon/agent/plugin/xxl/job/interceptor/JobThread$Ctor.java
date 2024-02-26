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

import com.xxl.job.core.biz.model.TriggerParam;
import org.bithon.agent.instrumentation.aop.IBithonObject;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AfterInterceptor;
import org.bithon.agent.observability.tracing.context.ITraceContext;
import org.bithon.agent.observability.tracing.context.TraceContextHolder;
import org.bithon.component.commons.utils.ReflectionUtils;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * {@link com.xxl.job.core.thread.JobThread}
 *
 * @author Frank Chen
 * @date 26/2/24 11:54 am
 */
public class JobThread$Ctor extends AfterInterceptor {

    @Override
    public void after(AopContext aopContext) throws Exception {
        // Replace the existing queue to support tracing
        ReflectionUtils.setFieldValue(aopContext.getTarget(), "triggerQueue", new Queue());
    }

    static class Queue extends LinkedBlockingQueue<TriggerParam> {
        @Override
        public boolean add(TriggerParam triggerParam) {

            // Keep the tracing context
            ITraceContext context = TraceContextHolder.current();
            if (context != null && triggerParam instanceof IBithonObject) {
                ((IBithonObject) triggerParam).setInjectedObject(new UserDefinedContext(context.currentSpan().traceId(),
                                                                                        context.currentSpan().spanId()));
            }

            return super.add(triggerParam);
        }

        @Override
        public TriggerParam poll(long timeout, TimeUnit unit) throws InterruptedException {
            // Make sure context is cleared on this thread
            UserDefinedContext.remove();

            TriggerParam param = super.poll(timeout, unit);

            // Restore tracing context
            if (param instanceof IBithonObject) {
                UserDefinedContext context = (UserDefinedContext) ((IBithonObject) param).getInjectedObject();

                UserDefinedContext.set(context.getTraceId(), context.getParentSpanId());
            }

            return param;
        }
    }
}
