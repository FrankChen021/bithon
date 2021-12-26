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
import org.bithon.agent.bootstrap.aop.InterceptionDecision;
import org.bithon.agent.core.tracing.context.ITraceSpan;
import org.bithon.agent.core.tracing.context.SpanKind;
import org.bithon.agent.core.tracing.context.Tags;
import org.bithon.agent.core.tracing.context.TraceSpanFactory;
import org.bithon.component.logging.ILogAdaptor;
import org.bithon.component.logging.LoggerFactory;
import redis.clients.jedis.Client;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

/**
 * @author frankchen
 */
public class JedisClientTraceHandler extends AbstractInterceptor {
    private static final ILogAdaptor log = LoggerFactory.getLogger(JedisClientTraceHandler.class);
    //TODO: move the configuration
    private final Set<String> ignoreCommands = new HashSet<>();

    @Override
    public boolean initialize() {
        ignoreCommands.add("AUTH");
        ignoreCommands.add("Protocol.Command.SELECT");
        ignoreCommands.add("Protocol.Command.ECHO");
        ignoreCommands.add("Protocol.Command.QUIT");
        return true;
    }

    @Override
    public InterceptionDecision onMethodEnter(AopContext aopContext) {
        String command = aopContext.getArgs()[0].toString();
        if (ignoreCommand(command)) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        ITraceSpan span = TraceSpanFactory.newSpan("jedis");
        if (span == null) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        Client redisClient = aopContext.castTargetAs();
        String uri = "redis://" + redisClient.getHost() + ":" + redisClient.getPort();

        aopContext.setUserContext(span.method(command)
                                      .kind(SpanKind.CLIENT)
                                      .tag(Tags.URI, uri)
                                      .start());

        return InterceptionDecision.CONTINUE;
    }

    @Override
    public void onMethodLeave(AopContext aopContext) {
        ITraceSpan span = aopContext.castUserContextAs();

        String[] params = this.parseParams(aopContext.getArgs());
        for (int i = 0; params != null && i < params.length; i++) {
            span.tag("param" + (i + 1), params[i]);
        }
        span.finish();
    }

    private boolean ignoreCommand(String command) {
        return this.ignoreCommands.contains(command);
    }

    private String[] parseParams(Object[] objects) {
        String[] strs = null;
        if (objects != null && objects.length >= 2 && objects[1] instanceof byte[][]) {
            byte[][] byteArray = (byte[][]) objects[1];
            strs = new String[byteArray.length];
            for (int i = 0; i < byteArray.length; i++) {
                try {
                    strs[i] = new String(byteArray[i], StandardCharsets.UTF_8);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
        return strs;
    }
}
