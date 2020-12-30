//package com.keruyun.commons.agent.plugin.mysql;
//
//import brave.Span;
//import brave.Tracer;
//import com.keruyun.ContextHolder;
//import com.keruyun.SpanScopeHolder;
//import com.keruyun.TraceHolder;
//import com.keruyun.commons.agent.core.interceptor.EventCallback;
//import com.keruyun.commons.agent.core.model.aop.AfterJoinPoint;
//import com.keruyun.commons.agent.core.model.aop.BeforeJoinPoint;
//import shaded.org.slf4j.Logger;
//import shaded.org.slf4j.LoggerFactory;
//
//import java.util.*;
//import java.util.stream.Collectors;
//
///**
// * Description :
// * <br>Date: 18/5/8
// *
// * @author 马至远
// */
//public class PreparedStatementTraceHandler extends EventCallback {
//    private static final Logger log = LoggerFactory.getLogger(PreparedStatementTraceHandler.class);
//    private static final String KEY = "mysql";
//    private static final String KEY_IGNORED_SUFFIXES = "ignoredSuffixes";
//    Set<String> ignoredSuffixes ;
//    @Override
//    public boolean init() throws Exception {
//        ignoredSuffixes = containsAttribute(KEY_IGNORED_SUFFIXES)
//                ?
//                Arrays.stream(getAttribute(KEY_IGNORED_SUFFIXES).toString().split(",")).map(x -> x.trim().toLowerCase()).collect(Collectors.toSet())
//                :
//                new HashSet<>(0);
//        return true;
//    }
//    @Override
//    protected Object createContext(BeforeJoinPoint joinPoint) {
//        return System.nanoTime();
//    }
//    protected void before(BeforeJoinPoint joinPoint) {
//        Tracer trace = null;
//        Tracer.SpanInScope scope = null;
//        try{
//            trace = TraceHolder.get();
//            if(trace != null){
//                Span span = trace.currentSpan();
//                if(span != null){
//                    /*if(joinPoint.getArgs() != null && joinPoint.getArgs().length > 0 && needIgnore(joinPoint.getArgs()[0].toString())){
//                        return ;
//                    }*/
//                    Span mysql = trace.newChild(span.context()).name(KEY).start();
//                    scope = trace.withSpanInScope(mysql);
//                    SpanScopeHolder.set(KEY,scope);
//                }
//
//            }
//        }catch (Exception e){
//            log.error(e.getMessage(),e);
//        }
//    }
//
//    @Override
//    protected void after(AfterJoinPoint joinPoint) {
//        Tracer trace = null;
//        try{
//            trace = TraceHolder.get();
//            if(trace != null){
//                Span span = trace.currentSpan();
//                if(span != null){
//                    /*if(joinPoint.getArgs() != null && joinPoint.getArgs().length > 0 && needIgnore(joinPoint.getArgs()[0].toString())){
//                        return ;
//                    }*/
//                    span.tag("class",joinPoint.getTarget().getClass().getName());
//                    span.tag("method",joinPoint.getMethod().getName());
//                    /*if(joinPoint.getArgs() != null && joinPoint.getArgs().length > 0){
//                        span.tag("sql",joinPoint.getArgs()[0].toString());
//                    }*/
//                    String sql = null;
//                    if((sql = (String) ContextHolder.get(ConnectionTraceHandler.KEY)) != null){
//                        span.tag(ConnectionTraceHandler.KEY,sql);
//                    }
//                    span.finish();
//                }
//
//            }
//        }catch (Exception e){
//            log.error(e.getMessage(),e);
//        }finally {
//            try{
//                Tracer.SpanInScope scope = SpanScopeHolder.get(KEY);
//                if(scope != null){
//                    scope.close();
//                }
//                SpanScopeHolder.remove(KEY);
//                ContextHolder.remove(ConnectionTraceHandler.KEY);
//            }catch (Exception e){
//                log.error(e.getMessage(),e);
//            }
//        }
//    }
//    private boolean needIgnore(String sql) {
//        if(sql.contains("@@session")){
//            return true;
//        }
//        return ignoredSuffixes.contains(sql);
//    }
//}
