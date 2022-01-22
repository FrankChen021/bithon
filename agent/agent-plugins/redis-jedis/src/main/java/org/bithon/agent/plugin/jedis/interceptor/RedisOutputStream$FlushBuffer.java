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

import org.bithon.agent.bootstrap.aop.AbstractInterceptor;
import org.bithon.agent.bootstrap.aop.AopContext;
import org.bithon.agent.core.context.InterceptorContext;
import org.bithon.agent.core.utils.ReflectionUtils;
import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.LoggerFactory;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;

/**
 * @author frankchen
 * @date Dec 27, 2020 11:14:08 PM
 */
public class RedisOutputStream$FlushBuffer extends AbstractInterceptor {
    private static final ILogAdaptor log = LoggerFactory.getLogger(RedisOutputStream$FlushBuffer.class);

    @Override
    public void onConstruct(AopContext aopContext) throws Exception {
        OutputStream os = aopContext.getArgAs(0);

        try {
            Field outputStreamField = ReflectionUtils.getField(aopContext.getTarget().getClass(), "out");
            outputStreamField.setAccessible(true);
            outputStreamField.set(aopContext.getTarget(), new OutputStreamDecorator(os));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            log.error("Unable to set OutputStream for RedisOutputStream: {}", e.getMessage());
        }
    }

    private static class OutputStreamDecorator extends FilterOutputStream {
        public OutputStreamDecorator(OutputStream out) {
            super(out);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            super.write(b, off, len);

            try {
                JedisContext ctx = InterceptorContext.getAs("redis-command");
                if (ctx != null) {
                    ctx.getMetrics().addRequestBytes(len);
                } else {
                    log.warn("Redis command is not instrumented", new RuntimeException());
                }
            } catch (Throwable ignored) {
            }
        }
    }
}
