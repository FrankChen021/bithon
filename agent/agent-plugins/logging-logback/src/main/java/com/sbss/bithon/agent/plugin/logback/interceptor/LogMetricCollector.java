package com.sbss.bithon.agent.plugin.logback.interceptor;

import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import com.sbss.bithon.agent.core.dispatcher.IMessageConverter;
import com.sbss.bithon.agent.core.metric.collector.IMetricCollector;
import com.sbss.bithon.agent.core.metric.domain.exception.ExceptionMetricSet;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

/**
 * @author frankchen
 */
class LogMetricCollector implements IMetricCollector {

    private final Queue<AppException> exceptionList = new ConcurrentLinkedDeque<>();

    public void addException(String uri, IThrowableProxy exception) {
        exceptionList.offer(new AppException(uri, exception));
    }

    @Override
    public boolean isEmpty() {
        return exceptionList.isEmpty();
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
    public List<Object> collect(IMessageConverter messageConverter,
                                int interval,
                                long now) {
        Map<String, ExceptionMetricSet> metricMap = new HashMap<>();

        //
        // merge exception together
        //
        AppException appException;
        do {
            appException = exceptionList.poll();
            if (null == appException) {
                break;
            }

            ExceptionMetricSet counter = appException.toExceptionCounter();

            metricMap.computeIfAbsent(appException.getUri() + counter.getExceptionId(), key -> counter)
                     .incrCount();

        } while (appException.getTimestamp() <= now);

        if (metricMap.isEmpty()) {
            return Collections.emptyList();
        }

        return metricMap.values()
                        .stream()
                        .map(metric -> messageConverter.from(now, interval, metric))
                        .collect(Collectors.toList());
    }

    private static class AppException {
        private final long timestamp;
        private final String uri;
        private final IThrowableProxy exception;

        AppException(String uri, IThrowableProxy exception) {
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

        public IThrowableProxy getException() {
            return exception;
        }

        public ExceptionMetricSet toExceptionCounter() {
            return new ExceptionMetricSet(uri,
                                          exception.getClassName(),
                                          exception.getMessage(),
                                          getFullStack(exception.getStackTraceElementProxyArray()));
        }

        private String getFullStack(StackTraceElementProxy[] stacks) {
            StringBuilder sb = new StringBuilder();
            if (stacks != null && stacks.length > 0) {
                for (StackTraceElementProxy msg : stacks) {
                    sb.append(msg.toString()).append("\r\n");
                }
            }
            return sb.toString();
        }
    }
}
