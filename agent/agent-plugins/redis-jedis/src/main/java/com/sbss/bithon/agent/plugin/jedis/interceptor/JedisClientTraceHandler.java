package com.sbss.bithon.agent.plugin.jedis.interceptor;

import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AbstractInterceptor;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AopContext;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.InterceptionDecision;
import com.sbss.bithon.agent.core.tracing.context.SpanKind;
import com.sbss.bithon.agent.core.tracing.context.TraceContext;
import com.sbss.bithon.agent.core.tracing.context.TraceContextHolder;
import com.sbss.bithon.agent.core.tracing.context.TraceSpan;
import redis.clients.jedis.Client;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

/**
 * @author frankchen
 */
public class JedisClientTraceHandler extends AbstractInterceptor {
    private static final Logger log = LoggerFactory.getLogger(JedisClientTraceHandler.class);
    private Set<String> ignoreCommands = new HashSet<String>();

    @Override
    public boolean initialize() {
        ignoreCommands.add("AUTH");
        ignoreCommands.add("Protocol.Command.SELECT");
        ignoreCommands.add("Protocol.Command.ECHO");
        ignoreCommands.add("Protocol.Command.QUIT");
        return true;
    }

    @Override
    public InterceptionDecision onMethodEnter(AopContext aopContext) throws Exception {
        String command = aopContext.getArgs()[0].toString();
        if (ignoreCommand(command)) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        TraceContext tracer = TraceContextHolder.get();
        if (tracer == null) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        TraceSpan parentSpan = tracer.currentSpan();
        if (parentSpan == null) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        Client redisClient = aopContext.castTargetAs();
        String hostAndPort = redisClient.getHost() + ":" + redisClient.getPort();

        TraceSpan thisSpan = parentSpan.newChildSpan("jedis")
                                       .clazz(aopContext.getTargetClass())
                                       .method(command)
                                       .kind(SpanKind.CLIENT)
                                       .tag("uri", hostAndPort)
                                       .start();
        aopContext.setUserContext(thisSpan);

        return InterceptionDecision.CONTINUE;
    }

    @Override
    public void onMethodLeave(AopContext aopContext) throws Exception {
        TraceSpan span = aopContext.castUserContextAs();
        if (span == null) {
            return;
        }

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
