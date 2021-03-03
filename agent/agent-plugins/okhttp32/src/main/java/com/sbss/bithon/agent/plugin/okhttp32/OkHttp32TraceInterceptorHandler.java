//package com.sbss.apm.javaagent.plugin.okhttp32;
//
//import com.sbss.SpanScopeHolder;
//import com.sbss.TraceHolder;
//import com.sbss.TracingHolder;
//import com.sbs.apm.javaagent.core.interceptor.EventCallback;
//import com.sbs.apm.javaagent.core.model.aop.AopContext;
//import com.sbs.apm.javaagent.core.model.aop.AopContext;
//
//import brave.Span;
//import brave.Tracer;
//import brave.Tracing;
//import brave.propagation.Propagation;
//import brave.propagation.TraceContextOrSamplingFlags;
//import okhttp3.Interceptor;
//import shaded.org.slf4j.Logger;
//import shaded.org.slf4j.LoggerFactory;
//
//public class OkHttp32TraceInterceptorHandler extends EventCallback {
//    private static final Logger log = LoggerFactory.getLogger(OkHttp32TraceInterceptorHandler.class);
//    private static final String KEY = "okhttp";
//    @Override
//    public boolean init() throws Exception {
//        return true;
//    }
//    @Override
//    protected void before(AopContext joinPoint) {
//        Tracer trace = null;
//        Tracer.SpanInScope scope = null;
//        try{
//            Interceptor.Chain chain = (Interceptor.Chain)joinPoint.getArgs()[0];
//            okhttp3.Request request = chain.request();
//            Tracing tracing = TracingHolder.getTracing();
//            trace = TraceHolder.get();
//            if(trace != null){
//                Span span = trace.currentSpan();
//                if(span != null){
//                    Span http = trace.newChild(span.context()).name("okhttp").start();
//                    http.kind(Span.Kind.CLIENT);
//                    http.tag("uri",request.url().uri().toString());
//                    scope = trace.withSpanInScope(http);
//                    SpanScopeHolder.set(KEY,scope);
//
//                }
//
//            }else{
//              //okhttp异步请求
//                tracing = TracingHolder.getTracing();
//                TraceContextOrSamplingFlags extractor = tracing.propagation().extractor(new Propagation.Getter<okhttp3.Request,String>() {
//                    public String get(okhttp3.Request carrier, String key){
//                        return carrier.header(key);
//                    }
//                }).extract(request);
//                if(extractor.context() != null){
//                    trace = tracing.tracer();
//                    Span span = trace.nextSpan(extractor).name("okhttp").start();
//                    span.tag("uri",request.url().uri().toString());
//                    span.kind(Span.Kind.SERVER);
//                    scope = trace.withSpanInScope(span);
//                    SpanScopeHolder.set(KEY,scope);
//                    TraceHolder.set(trace);
//                    TraceHolder.setAsy(true);
//                }
//            }
//
//        }catch (Exception e){
//            log.error(e.getMessage(),e);
//        }
//    }
//
//    @Override
//    protected void after(AopContext joinPoint) {
//        Tracer trace = null;
//        try{
//            trace = TraceHolder.get();
//            Boolean asy = TraceHolder.getAsy();
//
//            if(asy != null && asy){
//                TraceHolder.set(null);
//                TraceHolder.setAsy(null);
//            }
//            if(trace != null){
//                Span span = trace.currentSpan();
//                if(span != null){
//                    okhttp3.Response response = (okhttp3.Response)joinPoint.getResult();
//                    if(response != null){
//                        span.tag("status",new Integer(response.code()).toString());
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
//}
