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

package org.bithon.agent.plugin.redis.lettuce.interceptor;

import io.lettuce.core.protocol.AsyncCommand;
import org.bithon.agent.instrumentation.aop.IBithonObject;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AfterInterceptor;
import org.bithon.agent.observability.metric.domain.redis.RedisMetricRegistry;
import org.bithon.agent.plugin.redis.lettuce.LettuceAsyncContext;

/**
 * {@link io.lettuce.core.protocol.AsyncCommand#completeResult()}
 *
 * @author frankchen
 */
public class AsyncCommand$Complete extends AfterInterceptor {

    private final RedisMetricRegistry metricRegistry = RedisMetricRegistry.get();

    @Override
    public void after(AopContext aopContext) {
        if (!(aopContext.getTarget() instanceof IBithonObject)) {
            return;
        }

        LettuceAsyncContext asyncContext = (LettuceAsyncContext) ((IBithonObject) aopContext.getTarget()).getInjectedObject();

        if (asyncContext != null &&
            asyncContext.getDimensions() != null &&
            asyncContext.getStartTime() != null) {

            AsyncCommand<?, ?, ?> asyncCommand = (AsyncCommand<?, ?, ?>) aopContext.getTarget();

            boolean fail = "cancel".equals(aopContext.getMethod())
                           || asyncCommand.getOutput().hasError()
                           || "doCompleteExceptionally".equals(aopContext.getMethod());

            this.metricRegistry.getOrCreateMetrics(asyncContext.getDimensions())
                               .addRequest(System.nanoTime() - asyncContext.getStartTime(), fail ? 1 : 0)
                               .addRequestBytes(asyncContext.getRequestSize())
                               .addResponseBytes(asyncContext.getResponseSize());

        }
    }
}
