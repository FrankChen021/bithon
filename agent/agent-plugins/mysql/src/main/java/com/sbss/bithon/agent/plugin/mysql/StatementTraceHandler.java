//package com.keruyun.commons.agent.plugin.mysql;
//
//import java.util.Arrays;
//import java.util.HashSet;
//import java.util.Set;
//import java.util.stream.Collectors;
//
//import com.keruyun.SpanScopeHolder;
//import com.keruyun.TraceHolder;
//import com.sbs.apm.javaagent.core.interceptor.EventCallback;
//import com.sbs.apm.javaagent.core.model.aop.AfterJoinPoint;
//import com.sbs.apm.javaagent.core.model.aop.BeforeJoinPoint;
//
//import brave.Span;
//import brave.Tracer;
//import shaded.org.slf4j.Logger;
//import shaded.org.slf4j.LoggerFactory;
//
//public class StatementTraceHandler extends EventCallback {
//    private static final Logger log = LoggerFactory.getLogger(StatementTraceHandler.class);
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
//    protected void before(BeforeJoinPoint joinPoint) {
//        Tracer trace = null;
//        Tracer.SpanInScope scope = null;
//        try{
//            trace = TraceHolder.get();
//            if(trace != null){
//                Span span = trace.currentSpan();
//                if(span != null){
//                    if(needIgnore(joinPoint.getArgs()[0].toString())){
//                        return ;
//                    }
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
//                    if(joinPoint.getArgs() != null && joinPoint.getArgs().length > 0 && needIgnore(joinPoint.getArgs()[0].toString())){
//                        return ;
//                    }
//                    span.tag("class",joinPoint.getTarget().getClass().getName());
//                    span.tag("method",joinPoint.getMethod().getName());
//                    if(joinPoint.getArgs() != null && joinPoint.getArgs().length > 0){
//                        span.tag("sql",joinPoint.getArgs()[0].toString());
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
