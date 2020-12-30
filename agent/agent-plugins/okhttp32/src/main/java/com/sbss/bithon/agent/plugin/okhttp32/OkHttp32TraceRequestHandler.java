//package com.sbss.apm.javaagent.plugin.okhttp32;
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
//import okhttp3.Request;
//import shaded.org.slf4j.Logger;
//import shaded.org.slf4j.LoggerFactory;
//
///**
// * Created by shiweilu on 2018/7/25.
// */
//public class OkHttp32TraceRequestHandler extends EventCallback {
//    private static final Logger log = LoggerFactory.getLogger(OkHttp32TraceRequestHandler.class);
//    private static final String KEY = "okhttp";
//    @Override
//    public boolean init() throws Exception {
//        return true;
//    }
//    @Override
//    protected void before(BeforeJoinPoint joinPoint) {
//        Tracer trace = null;
//        Tracer.SpanInScope scope = null;
//        try{
//            okhttp3.Request.Builder builder = (okhttp3.Request.Builder)joinPoint.getTarget();
//            Tracing tracing = TracingHolder.getTracing();
//            trace = TraceHolder.get();
//            if(trace != null){
//                Span span = trace.currentSpan();
//                if(span != null){
//                    tracing.propagation().injector(new Propagation.Setter<Request.Builder, String>() {
//                        public void put(Request.Builder request, String key, String value){
//                            request.header(key,value);
//                        }
//                    }).inject(span.context(),builder);
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
//
//    }
//}
