package com.sbss.bithon.agent.plugin.log4j2;

import com.sbss.bithon.agent.core.context.AppInstance;
import com.sbss.bithon.agent.core.dispatcher.IMessageConverter;
import com.sbss.bithon.agent.core.metrics.IMetricProvider;
import com.sbss.bithon.agent.core.metrics.exception.ExceptionMetric;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * @author frank.chen021@outlook.com
 */
public class LogMetricProvider implements IMetricProvider {

    private static class AppException {
        private final long timestamp;
        private final String uri;
        private final Throwable exception;

        AppException(String uri, Throwable exception) {
            this.uri = uri;
            this.timestamp = System.currentTimeMillis();
            this.exception = exception;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public String getUri() {
            return uri;
        }

        public Throwable getException() {
            return exception;
        }

        public ExceptionMetric toExceptionCounter() {
            return ExceptionMetric.fromException(uri, exception);
        }
    }

    private Queue<AppException> exceptionList = new ConcurrentLinkedDeque<>();

    public void addException(String uri, Throwable exception) {
        exceptionList.offer(new AppException(uri, exception));
    }

//    private void addTrace(String msg) {
//        Tracer trace = null;
//        Tracer.SpanInScope scope = null;
//        try{
//            trace = TraceHolder.get();
//            if(trace != null){
//                Span span = trace.currentSpan();
//                if(span != null){
//                    Span s = trace.newChild(span.context()).name(KEY).start();
//                    s.tag("error",msg);
//                    s.finish();
//                }
//
//            }
//        }catch (Exception ex){
//            //log.error(e.getMessage(),e);
//        }
//    }

    @Override
    public boolean isEmpty() {
        return exceptionList.isEmpty();
    }

    @Override
    public List<Object> buildMessages(IMessageConverter messageConverter,
                                      AppInstance appInstance,
                                      int interval,
                                      long now) {
        Map<String, ExceptionMetric> counters = new HashMap<>();

        //
        // merge exception together
        //
        AppException appException;
        do {
            appException = exceptionList.poll();
            if (null == appException) {
                break;
            }

            ExceptionMetric counter = appException.toExceptionCounter();

            counters.computeIfAbsent(appException.getUri() + counter.getExceptionId(), key -> counter)
                .incrCount();

        } while (appException.getTimestamp() <= now);

        if (counters.isEmpty()) {
            return Collections.emptyList();
        }

        return messageConverter.from(appInstance, now, interval, counters.values());
    }
}
