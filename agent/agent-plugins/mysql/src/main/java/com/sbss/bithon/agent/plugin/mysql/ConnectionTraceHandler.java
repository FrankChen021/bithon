package com.sbss.bithon.agent.plugin.mysql;

import com.sbss.bithon.agent.core.context.ContextHolder;
import com.sbss.bithon.agent.core.interceptor.AbstractMethodIntercepted;
import com.sbss.bithon.agent.core.interceptor.BeforeJoinPoint;

public class ConnectionTraceHandler extends AbstractMethodIntercepted {
    public static final String KEY = "sql";
    public static final String SLOWKEY = "slowSql";

    @Override
    public boolean init() throws Exception {
        return true;
    }

    @Override
    protected void before(BeforeJoinPoint joinPoint) {
        if (joinPoint.getArgs() != null && joinPoint.getArgs().length > 0) {
            ContextHolder.set(SLOWKEY, joinPoint.getArgs()[0].toString());
        }
//        Tracer trace = null;
//        try{
//            trace = TraceHolder.get();
//            if(trace != null){
//                Span span = trace.currentSpan();
//                if(span != null){
//                    if(joinPoint.getArgs() != null && joinPoint.getArgs().length > 0){
//                        ContextHolder.set(KEY,joinPoint.getArgs()[0].toString());
//                    }
//                }
//
//            }
//        }catch (Exception e){
//            log.error(e.getMessage(),e);
//        }
    }
}
