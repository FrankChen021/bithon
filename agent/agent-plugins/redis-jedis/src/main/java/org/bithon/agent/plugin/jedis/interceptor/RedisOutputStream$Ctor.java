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
import org.bithon.agent.instrumentation.aop.interceptor.declaration.BeforeInterceptor;
import org.bithon.agent.observability.context.InterceptorContext;
import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.LoggerFactory;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * {@link redis.clients.util.RedisOutputStream#RedisOutputStream(OutputStream out, int size)}
 *
 * @author frankchen
 * @date Dec 27, 2020 11:14:08 PM
 */
public class RedisOutputStream$Ctor extends BeforeInterceptor {
    private static final ILogAdaptor log = LoggerFactory.getLogger(RedisOutputStream$Ctor.class);

    @Override
    public void before(AopContext aopContext) {
        OutputStream os = aopContext.getArgAs(0);
        if (os != null) {
        /*
        try {
            Field outputStreamField = ReflectionUtils.getField(aopContext.getTarget().getClass(), "out");
            outputStreamField.setAccessible(true);
            outputStreamField.set(aopContext.getTarget(), new OutputStreamDecorator(os));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            log.error("Unable to set OutputStream for RedisOutputStream: {}", e.getMessage());
        }*/
            aopContext.getArgs()[0] = new OutputStreamDecorator(os);
        }
    }

    private static class OutputStreamDecorator extends FilterOutputStream {
        public OutputStreamDecorator(OutputStream out) {
            super(out);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            // Call the target's write method directly instead of the super's because the super's implementation has very poor performance
            out.write(b, off, len);

            try {
                JedisContext ctx = InterceptorContext.getAs("redis-command");
                if (ctx != null) {
                    ctx.getMetrics().addRequestBytes(len);
                } else {
                    // log the stack trace to help get known which command is not instrumented
                    log.warn("Redis command is not instrumented.", new RuntimeException());
                }
            } catch (Throwable ignored) {
            }
        }
    }
}
