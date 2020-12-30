//package com.sbss.apm.javaagent.plugin.springweb;
//
//import java.util.ArrayList;
//import java.util.List;
//
//import com.keruyun.TraceHolder;
//import com.keruyun.TracingHolder;
//import com.sbs.apm.javaagent.core.interceptor.EventCallback;
//import com.sbs.apm.javaagent.core.model.aop.AfterJoinPoint;
//import com.sbs.apm.javaagent.core.model.aop.BeforeJoinPoint;
//
//import brave.Span;
//import brave.Tracer;
//import brave.Tracing;
//import brave.propagation.Propagation;
//import feign.Request;
//import shaded.org.slf4j.Logger;
//import shaded.org.slf4j.LoggerFactory;
//
///**
// * Created by shiweilu on 2018/7/25.
// */
//public class FeignPropRequestHandler extends EventCallback {
//    private static final String KEY = "feignClient";
//    private static final Logger log = LoggerFactory.getLogger(FeignPropRequestHandler.class);
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
//                    tracing.propagation().injector(new Propagation.Setter<Request, String>() {
//                        public void put(Request request, String key, String value){
//                            List list = new ArrayList<>();
//                            list.add(value);
//                            if(!request.headers().containsKey(key)){
//                                request.headers().put(key,list);
//                            }
//
//                        }
//                    }).inject(span.context(),request);
//                }
//            }
//        }catch (Exception e){
//            log.error(e.getMessage(),e);
//        }
//    }
//    @Override
//    protected void after(AfterJoinPoint joinPoint) {
//
//    }
//
//
//}
