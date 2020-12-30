//package com.keruyun.commons.agent.plugin.tomcat;
//
//import brave.Span;
//import brave.Tracer;
//import brave.Tracing;
//import brave.propagation.Propagation;
//import brave.propagation.TraceContext;
//import brave.propagation.TraceContextOrSamplingFlags;
//import brave.sampler.CountingSampler;
//import com.keruyun.*;
//import com.keruyun.commons.agent.core.interceptor.EventCallback;
//import com.keruyun.commons.agent.core.model.aop.AfterJoinPoint;
//import com.keruyun.commons.agent.core.model.aop.BeforeJoinPoint;
//import com.keruyun.commons.agent.dispatcher.GolableConfig;
//import org.apache.catalina.connector.Request;
//import shaded.org.slf4j.Logger;
//import shaded.org.slf4j.LoggerFactory;
//
//import java.util.Arrays;
//import java.util.HashSet;
//import java.util.Set;
//import java.util.stream.Collectors;
//
///**
// * Created by shiweilu on 2018/7/17.
// */
//public class TomcatTraceHandler extends EventCallback {
//    private static final Logger log = LoggerFactory.getLogger(TomcatTraceHandler.class);
//    private static final String KEY = "tomcat";
//    private static final String TRACEID = "traceId";
//    private static final String SAMPLE = "X-B3-Sampled";
//    private static final String KEY_IGNORED_SUFFIXES = "ignoredSuffixes";
//    private static final String URI = "uri";
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
//
//    protected void before(BeforeJoinPoint joinPoint) {
//        if(needIgnore(((org.apache.catalina.connector.Request)joinPoint.getArgs()[0]).getRequestURI())){
//            return;
//        }
//        try{
//            org.apache.catalina.connector.Request request = (org.apache.catalina.connector.Request)joinPoint.getArgs()[0];
//            ContextHolder.set(URI,request.getRequestURI());
//            Tracing tracing = TracingHolder.getTracing();
//            TraceContextOrSamplingFlags extractor = tracing.propagation().extractor(new Propagation.Getter<Request,String>() {
//                public String get(Request carrier, String key){
//                    if(key.equals(SAMPLE)&&carrier.getHeader(key) == null){
//                        if(GolableConfig.getConfig().isTrace() && SamplerUtils.staticSample()){
//                            return "1";
//                        }else{
//                            return "0";
//                        }
//                    }
//                    return carrier.getHeader(key);
//                }
//            }).extract(request);
//            String traceId = request.getHeader(TRACEID);
//            if(traceId != null && !"".equals(traceId.trim())){
//                traceId = traceId.replace("-","");
//                ContextHolder.set(TRACEID,traceId);
//            }
//            Tracer trace = tracing.tracer();
//            if(trace != null){
//                Span span = trace.nextSpan(extractor).name(KEY).start();
//                span.tag("uri",request.getRequestURI());
//                span.kind(Span.Kind.SERVER);
//                Tracer.SpanInScope scope = trace.withSpanInScope(span);
//                SpanScopeHolder.set(KEY,scope);
//                TraceHolder.set(trace);
//            }
//        }catch (Exception e){
//            log.error(e.getMessage(),e);
//        }
//
//    }
//    @Override
//    protected void after(AfterJoinPoint joinPoint) {
//        if(needIgnore(((org.apache.catalina.connector.Request)joinPoint.getArgs()[0]).getRequestURI())){
//            return;
//        }
//        Span span = null;
//        Tracer tracer = null;
//        try{
//            ContextHolder.remove(URI);
//            tracer = TraceHolder.get();
//            if(tracer != null){
//                span = tracer.currentSpan();
//                if(span != null){
//                    org.apache.catalina.connector.Response response = (org.apache.catalina.connector.Response)joinPoint.getArgs()[1];
//                    span.tag("status",new Integer(response.getStatus()).toString());
//                    span.finish();
//                }
//            }
//        }catch (Exception e){
//            log.error(e.getMessage(),e);
//        }finally {
//            TraceHolder.set(null);
//            Tracer.SpanInScope scope = SpanScopeHolder.get(KEY);
//            if(scope != null){
//                scope.close();
//            }
//            SpanScopeHolder.remove(KEY);
//        }
//
//    }
//    private boolean needIgnore(String uri) {
//        String suffix = uri.substring(uri.lastIndexOf(".") + 1).toLowerCase();
//        return ignoredSuffixes.contains(suffix);
//    }
//}
