//package com.sbss.apm.javaagent.plugin.jedis;
//
//import java.util.HashSet;
//import java.util.Set;
//
//import com.keruyun.SpanScopeHolder;
//import com.keruyun.TraceHolder;
//import com.sbs.apm.javaagent.core.interceptor.EventCallback;
//import com.sbs.apm.javaagent.core.model.aop.AfterJoinPoint;
//import com.sbs.apm.javaagent.core.model.aop.BeforeJoinPoint;
//
//import brave.Span;
//import brave.Tracer;
//import redis.clients.jedis.Protocol;
//import shaded.org.slf4j.Logger;
//import shaded.org.slf4j.LoggerFactory;
//
///**
// * Description : Jedis plugin <br>
// * Date: 17/11/1
// *
// * @author 马至远
// */
//public class JedisClientTraceHandler extends EventCallback {
//    private static final Logger log = LoggerFactory.getLogger(JedisClientTraceHandler.class);
//    private static final String KEY = "redis";
//    private Set<Protocol.Command> ignoreSet = new HashSet<Protocol.Command>();
//
//    @Override
//    public boolean init() throws Exception {
//        ignoreSet.add(Protocol.Command.AUTH);
//        ignoreSet.add(Protocol.Command.SELECT);
//        ignoreSet.add(Protocol.Command.ECHO);
//        ignoreSet.add(Protocol.Command.QUIT);
//        return true;
//    }
//
//    @Override
//    protected void before(BeforeJoinPoint joinPoint) {
//        Tracer trace = null;
//        Tracer.SpanInScope scope = null;
//        try {
//            if (ignoreCommand(joinPoint.getArgs())) {
//                return;
//            }
//            trace = TraceHolder.get();
//            if (trace != null) {
//                Span span = trace.currentSpan();
//                if (span != null) {
//                    Span mysql = trace.newChild(span.context()).name(KEY).start();
//                    scope = trace.withSpanInScope(mysql);
//                    SpanScopeHolder.set(KEY, scope);
//                }
//
//            }
//        } catch (Exception e) {
//            log.error(e.getMessage(), e);
//        }
//    }
//
//    @Override
//    protected void after(AfterJoinPoint joinPoint) {
//        Tracer trace = null;
//        try {
//            if (ignoreCommand(joinPoint.getArgs())) {
//                return;
//            }
//            trace = TraceHolder.get();
//            if (trace != null) {
//                Span span = trace.currentSpan();
//                if (span != null) {
//                    // span.tag("class",joinPoint.getTarget().getClass().getName());
//                    // span.tag("method",joinPoint.getMethod().getName());
//                    span.tag("command", ((Protocol.Command) joinPoint.getArgs()[0]).toString());
//                    String[] params = this.parseParams(joinPoint.getArgs());
//                    for (int i = 0; params != null && i < params.length; i++) {
//                        span.tag("param" + (i + 1), params[i]);
//                    }
//                    span.finish();
//                }
//
//            }
//        } catch (Exception e) {
//            log.error(e.getMessage(), e);
//        } finally {
//            try {
//                Tracer.SpanInScope scope = SpanScopeHolder.get(KEY);
//                if (scope != null) {
//                    scope.close();
//                }
//                SpanScopeHolder.remove(KEY);
//            } catch (Exception e) {
//                log.error(e.getMessage(), e);
//            }
//        }
//    }
//
//    private boolean ignoreCommand(Object[] objects) {
//        if (objects != null && objects.length >= 1 && objects[0] instanceof Protocol.Command) {
//            return this.ignoreSet.contains(objects[0]) ? true : false;
//        }
//        return true;
//    }
//
//    private String[] parseParams(Object[] objects) {
//        String[] strs = null;
//        if (objects != null && objects.length >= 2 && objects[1] instanceof byte[][]) {
//            byte[][] byteArray = (byte[][]) objects[1];
//            strs = new String[byteArray.length];
//            for (int i = 0; i < byteArray.length; i++) {
//                try {
//                    strs[i] = new String(byteArray[i], "utf-8");
//                } catch (Exception e) {
//                    log.error(e.getMessage(), e);
//                }
//            }
//        }
//        return strs;
//    }
//}
