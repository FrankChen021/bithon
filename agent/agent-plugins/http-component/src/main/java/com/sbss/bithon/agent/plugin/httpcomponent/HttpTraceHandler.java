package com.sbss.bithon.agent.plugin.httpcomponent;

//package com.sbs.apm.javaagent.plugin.httpcomponent;
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
//import brave.propagation.Propagation;
//import shaded.org.slf4j.Logger;
//import shaded.org.slf4j.LoggerFactory;
//
///**
// *
// */
//public class HttpTraceHandler extends EventCallback {
//
//    private static final Logger log = LoggerFactory.getLogger(HttpTraceHandler.class);
//    private static final String KEY = "http";
//
//    @Override
//    public boolean init() throws Exception {
//        return true;
//    }
//
//    protected void before(BeforeJoinPoint joinPoint) {
//        Tracer trace = null;
//        Tracer.SpanInScope scope = null;
//        try{
//            org.apache.http.HttpRequest request = (org.apache.http.HttpRequest)joinPoint.getArgs()[0];
//            Tracing tracing = TracingHolder.getTracing();
//            trace = TraceHolder.get();
//            if(trace != null){
//                Span span = trace.currentSpan();
//                if(span != null){
//                    Span http = trace.newChild(span.context()).name("Httpclient").start();
//                    http.kind(Span.Kind.CLIENT);
//                    http.tag("uri",request.getRequestLine().getUri());
//                    scope = trace.withSpanInScope(http);
//                    SpanScopeHolder.set(KEY,scope);
//                    tracing.propagation().injector(new Propagation.Setter<org.apache.http.HttpRequest, String>() {
//                        public void put(org.apache.http.HttpRequest request, String key, String value){
//                            if(!request.containsHeader(key)){
//                                request.addHeader(key,value);
//                            }
//                        }
//                    }).inject(span.context(),request);
//                }
//
//            }
//        }catch (Exception e){
//            log.error(e.getMessage(),e);
//        }
//
//    }
//    @Override
//    protected void after(AfterJoinPoint joinPoint) {
//        Tracer trace = null;
//        try{
//            trace = TraceHolder.get();
//            if(trace != null){
//                Span span = trace.currentSpan();
//                if(span != null){
//                    org.apache.http.HttpResponse response = (org.apache.http.HttpResponse)joinPoint.getResult();
//                    span.tag("status",new Integer(response.getStatusLine().getStatusCode()).toString());
//                    span.finish();
//                }
//
//            }
//        }catch (Exception e){
//            log.error(e.getMessage(),e);
//        }finally {
//           try{
//               Tracer.SpanInScope scope = SpanScopeHolder.get(KEY);
//               if(scope != null){
//                   scope.close();
//               }
//               SpanScopeHolder.remove(KEY);
//           }catch (Exception e){
//               log.error(e.getMessage(),e);
//           }
//        }
//
//    }
//
//}
