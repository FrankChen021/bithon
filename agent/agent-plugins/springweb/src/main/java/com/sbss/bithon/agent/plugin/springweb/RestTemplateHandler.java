//package com.keruyun.commons.agent.plugin.springmvc;
//
//import brave.Span;
//import brave.Tracer;
//import com.keruyun.ContextHolder;
//import com.keruyun.SpanScopeHolder;
//import com.keruyun.TraceHolder;
//import com.keruyun.commons.agent.collector.entity.SpringRestfulUriPatternEntity;
//import com.keruyun.commons.agent.core.interceptor.EventCallback;
//import com.keruyun.commons.agent.core.model.aop.AfterJoinPoint;
//import com.keruyun.commons.agent.core.model.aop.BeforeJoinPoint;
//import com.keruyun.commons.agent.dispatcher.DispatchProcessor;
//import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
//import shaded.org.slf4j.Logger;
//import shaded.org.slf4j.LoggerFactory;
//
//import java.net.URI;
//import java.util.*;
//import java.util.concurrent.ConcurrentHashMap;
//
///**
// * Created by shiweilu on 2018/7/25.
// */
//public class RestTemplateHandler extends EventCallback {
//    private static final String KEY = "restTemplate";
//    private static final Logger log = LoggerFactory.getLogger(RestTemplateHandler.class);
//    @Override
//    public boolean init() throws Exception {
//        return true;
//    }
//    @Override
//    protected void before(BeforeJoinPoint joinPoint) {
//        Tracer trace = null;
//        Tracer.SpanInScope scope = null;
//        try{
//            trace = TraceHolder.get();
//            if(trace != null){
//                Span span = trace.currentSpan();
//                if(span != null){
//                    Span s = trace.newChild(span.context()).name(KEY).start();
//                    s.kind(Span.Kind.CLIENT);
//                    scope = trace.withSpanInScope(s);
//                    SpanScopeHolder.set(KEY,scope);
//                }
//
//            }
//        }catch (Exception e){
//            log.error(e.getMessage(),e);
//        }
//    }
//    @Override
//    protected void after(AfterJoinPoint joinPoint) {
//        Tracer trace = null;
//        try{
//            trace = TraceHolder.get();
//            if(trace != null){
//                Span span = trace.currentSpan();
//                if(span != null){
//                    Object obj = joinPoint.getArgs()[0];
//                    if(obj != null &&  obj instanceof  String){
//                        span.tag("url",(String) obj);
//                    }else if(obj != null && obj instanceof URI){
//                        span.tag("url",((URI) obj).toString());
//                    }
//                    span.finish();
//                }
//
//            }
//        }catch (Exception e){
//            log.error(e.getMessage(),e);
//        }finally {
//            try{
//                ContextHolder.remove(RequestHandler.FLAG);
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
//
//
//}
