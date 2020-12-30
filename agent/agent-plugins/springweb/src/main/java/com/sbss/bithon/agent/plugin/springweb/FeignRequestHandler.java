//package com.keruyun.commons.agent.plugin.springmvc;
//
//import com.keruyun.SpanScopeHolder;
//import com.keruyun.TraceHolder;
//import com.keruyun.TracingHolder;
//import com.sbs.apm.javaagent.core.interceptor.EventCallback;
//import com.sbs.apm.javaagent.core.model.aop.AfterJoinPoint;
//import com.sbs.apm.javaagent.core.model.aop.BeforeJoinPoint;
//
//import brave.Span;
//import brave.Tracer;
//import brave.Tracing;
//import feign.Request;
//import feign.Response;
//import shaded.org.slf4j.Logger;
//import shaded.org.slf4j.LoggerFactory;
//
///**
// * Created by shiweilu on 2018/7/25.
// */
//public class FeignRequestHandler extends EventCallback {
//    private static final String KEY = "feignClient";
//    private static final Logger log = LoggerFactory.getLogger(FeignRequestHandler.class);
//    @Override
//    public boolean init() throws Exception {
//        return true;
//    }
//    @Override
//    protected void before(BeforeJoinPoint joinPoint) {
//        Tracer trace = null;
//        Tracer.SpanInScope scope = null;
//        try{
//            Request request = (Request)joinPoint.getArgs()[0];
//            Tracing tracing = TracingHolder.getTracing();
//            trace = TraceHolder.get();
//            if(trace != null){
//                Span span = trace.currentSpan();
//                if(span != null){
//                    Span http = trace.newChild(span.context()).name("feignClient").start();
//                    http.kind(Span.Kind.CLIENT);
//                    http.tag("url",request.url());
//                    scope = trace.withSpanInScope(http);
//                    SpanScopeHolder.set(KEY,scope);
//                }
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
//                    Response response = (Response)joinPoint.getResult();
//                    span.tag("status",String.valueOf(response.status()));
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
//
//
//}
