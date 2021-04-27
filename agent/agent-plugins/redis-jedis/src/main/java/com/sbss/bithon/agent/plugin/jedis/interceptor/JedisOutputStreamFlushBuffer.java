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

package com.sbss.bithon.agent.plugin.jedis.interceptor;

import com.sbss.bithon.agent.bootstrap.aop.AbstractInterceptor;
import com.sbss.bithon.agent.bootstrap.aop.AopContext;
import com.sbss.bithon.agent.bootstrap.aop.IBithonObject;
import com.sbss.bithon.agent.bootstrap.aop.InterceptionDecision;
import com.sbss.bithon.agent.core.context.InterceptorContext;
import com.sbss.bithon.agent.core.metric.collector.MetricCollectorManager;
import com.sbss.bithon.agent.core.metric.domain.redis.RedisMetricCollector;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.lang.reflect.Field;

/**
 * @author frankchen
 * @date Dec 27, 2020 11:14:08 PM
 */
public class JedisOutputStreamFlushBuffer extends AbstractInterceptor {
    private static final Logger log = LoggerFactory.getLogger(JedisOutputStreamFlushBuffer.class);

    private Field countField;
    private RedisMetricCollector metricCollector;

    @Override
    public boolean initialize() {
        metricCollector = MetricCollectorManager.getInstance().getOrRegister("jedis", RedisMetricCollector.class);
        return true;
    }

    /**
     * count property will be flushed after execution of flushBuffer
     * so calculate the bytes before execution of the function
     * <p>
     * The endpoint is set in {@link JedisConnectionConnect} when OutputStream object is instantiated
     */
    @Override
    public InterceptionDecision onMethodEnter(AopContext context) throws Exception {
        // RedisOutputStream
        Object outputStream = context.getTarget();
        if (!ensureField(outputStream.getClass())) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        String endpoint = (String) ((IBithonObject) outputStream).getInjectedObject();
        String command = InterceptorContext.getAs("redis-command");
        int outputBytes = countField.getInt(outputStream);
        metricCollector.addOutputBytes(endpoint,
                                       command,
                                       outputBytes);

        return InterceptionDecision.SKIP_LEAVE;
    }

    private boolean ensureField(Class<?> clazz) {
        try {
            if (countField == null) {
                // since this function is lock free,
                // a temp variable is used to hold the result in order to prevent potential concurrent problems
                Field field = clazz.getDeclaredField("count");
                field.setAccessible(true);
                countField = field;
            }
            return true;
        } catch (NoSuchFieldException e) {
            log.warn("cannot access field [count] of RedisOutStream", e);
            return false;
        }
    }
}
