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
 * @author frankchen
 */
public class RedisAsyncCommandComplete extends AfterInterceptor {

    private final RedisMetricRegistry metricRegistry = RedisMetricRegistry.get();

    @Override
    public void after(AopContext aopContext) {
        if (!(aopContext.getTarget() instanceof IBithonObject)) {
            return;
        }

        LettuceAsyncContext asyncContext = (LettuceAsyncContext) ((IBithonObject) aopContext.getTarget()).getInjectedObject();

        if (asyncContext != null &&
            asyncContext.getEndpoint() != null &&
            asyncContext.getStartTime() != null) {
            AsyncCommand asyncCommand = (AsyncCommand) aopContext.getTarget();

            boolean fail = "cancel".equalsIgnoreCase(aopContext.getMethod()) || asyncCommand.getOutput()
                                                                                                      .hasError();

            //TODO: read/write
            //TODO: bytes
            this.metricRegistry.getOrCreateMetrics(asyncContext.getEndpoint(),
                                                   asyncCommand.getType().name())
                               .addRequest(System.nanoTime() - asyncContext.getStartTime(),
                                           fail ? 1 : 0);
        }
    }
}
