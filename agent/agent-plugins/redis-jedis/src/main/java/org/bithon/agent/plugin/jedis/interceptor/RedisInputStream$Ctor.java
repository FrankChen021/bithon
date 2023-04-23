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

package org.bithon.agent.plugin.jedis.interceptor;

import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AfterInterceptor;
import org.bithon.agent.observability.context.InterceptorContext;
import org.bithon.component.commons.logging.LoggerFactory;
import org.bithon.component.commons.utils.ReflectionUtils;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;

/**
 * {@link redis.clients.util.RedisInputStream#RedisInputStream}
 * @author frankchen
 * @date Dec 26, 2020 12:11:14 PM
 */
public class RedisInputStream$Ctor extends AfterInterceptor {

    @Override
    public void after(AopContext aopContext) {
        InputStream is = aopContext.getArgAs(0);

        try {
            Field inputStreamField = ReflectionUtils.getField(aopContext.getTarget().getClass(), "in");
            inputStreamField.setAccessible(true);
            inputStreamField.set(aopContext.getTarget(), new InputStreamDecorator(is));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            LoggerFactory.getLogger(RedisInputStream$Ctor.class).error("Unable to set InputStream for RedisInputStream: {}", e.getMessage());
        }
    }

    static class InputStreamDecorator extends FilterInputStream {
        public InputStreamDecorator(InputStream in) {
            super(in);
        }

        @Override
        public int read(byte[] b) throws IOException {
            int size = super.read(b);
            if (size <= 0) {
                return size;
            }
            try {
                JedisContext ctx = InterceptorContext.getAs("redis-command");
                if (ctx != null) {
                    ctx.getMetrics().addResponseBytes(size);
                }
            } catch (Throwable ignored) {

            }
            return size;
        }
    }
}
