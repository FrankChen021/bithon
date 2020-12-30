//package com.keruyun.commons.agent.plugin.springmvc;
//
//import java.util.ArrayList;
//import java.util.List;
//
//import org.springframework.http.HttpHeaders;
//
//import com.keruyun.TraceHolder;
//import com.keruyun.TracingHolder;
//import com.sbs.apm.javaagent.core.context.ContextHolder;
//import com.sbs.apm.javaagent.core.interceptor.EventCallback;
//import com.sbs.apm.javaagent.core.model.aop.AfterJoinPoint;
//import com.sbs.apm.javaagent.core.model.aop.BeforeJoinPoint;
//
//import brave.Span;
//import brave.Tracer;
//import brave.Tracing;
//import brave.propagation.Propagation;
//import shaded.org.slf4j.Logger;
//import shaded.org.slf4j.LoggerFactory;
//
///**
// * Created by shiweilu on 2018/7/25.
// */
//public class RequestHandler extends EventCallback {
//    private static final String KEY = "restTemplate";
//    public static final String FLAG = "flag";
//    private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);
//    @Override
//    public boolean init() throws Exception {
//        return true;
//    }
//    @Override
//    protected void before(BeforeJoinPoint joinPoint) {
//       /* Tracer trace = null;
//        Tracer.SpanInScope scope = null;
//        try{
//            ClientHttpRequest request = (ClientHttpRequest)joinPoint.getArgs()[0];
//            Tracing tracing = TracingHolder.getTracing();
//            trace = TraceHolder.get();
//            if(trace != null){
//                Span span = trace.currentSpan();
//                if(span != null){
//                    tracing.propagation().injector(new Propagation.Setter<ClientHttpRequest, String>() {
//                        public void put(ClientHttpRequest request, String key, String value){
//                            request.getHeaders().add(key,value);
//                        }
//                    }).inject(span.context(),request);
//                }
//            }
//        }catch (Exception e){
//            log.error(e.getMessage(),e);
//        }*/
//    }
//    @Override
//    protected void after(AfterJoinPoint joinPoint) {
//        Object result = joinPoint.getResult();
//        if(result != null && result instanceof HttpHeaders){
//            Tracer trace = null;
//            Tracer.SpanInScope scope = null;
//            try{
//                HttpHeaders header = (HttpHeaders)result;
//                Tracing tracing = TracingHolder.getTracing();
//                trace = TraceHolder.get();
//                if(trace != null){
//                    Span span = trace.currentSpan();
//                    if(span != null){
//                        if(ContextHolder.get(FLAG) == null){
//                            ContextHolder.set(FLAG,FLAG);
//                            tracing.propagation().injector(new Propagation.Setter<HttpHeaders, String>() {
//                                public void put(HttpHeaders header, String key, String value){
//                                    if(!header.containsKey(key)){
//                                        List<String> list = new ArrayList<>();
//                                        list.add(value);
//                                        header.put(key,list);
//                                    }
//                                }
//                            }).inject(span.context(),header);
//                        }
//                    }
//                }
//            }catch (Exception e){
//                log.error(e.getMessage(),e);
//            }
//
//        }
//    }
//
//
//}
