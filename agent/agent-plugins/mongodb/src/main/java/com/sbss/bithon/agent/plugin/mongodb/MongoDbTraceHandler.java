//package com.sbss.commons.agent.plugin.mongodb;
//
//import com.sbss.SpanScopeHolder;
//import com.sbss.TraceHolder;
//import com.sbs.apm.javaagent.core.interceptor.EventCallback;
//import com.sbs.apm.javaagent.core.model.aop.AopContext;
//import com.sbs.apm.javaagent.core.model.aop.AopContext;
//
//import brave.Span;
//import brave.Tracer;
//import shaded.org.slf4j.Logger;
//import shaded.org.slf4j.LoggerFactory;
//
// */
//public class MongoDbTraceHandler extends EventCallback {
//    private static final Logger log = LoggerFactory.getLogger(MongoDbTraceHandler.class);
//    private static final String KEY = "mongodb";
//    @Override
//    public boolean init() {
//        return true;
//    }
//
//    @Override
//    protected void before(AopContext joinPoint) {
//        Tracer trace = null;
//        Tracer.SpanInScope scope = null;
//        try{
//            /*if(ignoreCommand(joinPoint.getArgs())){
//                return;
//            }*/
//            trace = TraceHolder.get();
//            if(trace != null){
//                Span span = trace.currentSpan();
//                if(span != null){
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
//
//    @Override
//    protected void after(AopContext joinPoint) {
//
//        Tracer trace = null;
//        try{
//            /*if(ignoreCommand(joinPoint.getArgs())){
//                return;
//            }*/
//            trace = TraceHolder.get();
//            if(trace != null){
//                Span span = trace.currentSpan();
//                if(span != null){
//                    //span.tag("class",joinPoint.getTarget().getClass().getName());
//                    //span.tag("method",joinPoint.getMethod().getName());
//                    //span.tag("command",((Protocol.Command)joinPoint.getArgs()[0]).toString());
//                    /*String[] params = this.parseParams(joinPoint.getArgs());
//                    for( int i = 0;params != null && i < params.length;i++){
//                        span.tag("param"+i+1,params[i]);
//                    }*/
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
