package com.sbss.bithon.agent.plugin.logback;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import com.keruyun.commons.agent.collector.entity.ExceptionEntity;
import com.keruyun.commons.agent.collector.entity.FailureMessageDetailEntity;
import com.keruyun.commons.agent.collector.entity.FailureMessageEntity;
import com.sbss.bithon.agent.core.context.ContextHolder;
import com.sbss.bithon.agent.core.interceptor.AfterJoinPoint;
import com.sbss.bithon.agent.dispatcher.metrics.counter.IAgentCounter;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Description :
 * <br>Date: 18/4/13
 *
 * @author 马至远
 */
public class LogCounter implements IAgentCounter {
    private static final int ERROR_SOURCE_TYPE_LOGBACK = 3;
    private static final String KEY = "logback";
    private long earliestRecordTimestamp = 0, latestRecordTimestamp = 0;

    private static class ClientException {
        private String uri;
        private long timestamp;

        private IThrowableProxy rootException;

        ClientException(String uri, long occurTimestamp, IThrowableProxy throwable) {
            this.uri = uri;
            this.timestamp = occurTimestamp;
            this.rootException = throwable;
        }

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            ClientException that = (ClientException) o;

            return timestamp == that.timestamp && rootException.equals(that.rootException);
        }

        @Override
        public int hashCode() {
            int result = (int) (timestamp ^ (timestamp >>> 32));
            result = 31 * result + rootException.hashCode();
            return result;
        }

        long getTimestamp() {
            return timestamp;
        }

        IThrowableProxy getRootException() {
            return rootException;
        }
    }

    private Queue<ClientException> exceptionStorage = new ConcurrentLinkedDeque<>();

    /**
     * 目前拦截了log动作之后, 只对error类型的记录做了处理
     *
     * @param o 需要add的数据
     */
    @Override
    public void add(Object o) {
        AfterJoinPoint afterJoinPoint = (AfterJoinPoint) o;

        ILoggingEvent iLoggingEvent = (ILoggingEvent) afterJoinPoint.getArgs()[0];
        if (iLoggingEvent.getLevel().toInt() == Level.ERROR.toInt()) {
            IThrowableProxy iThrowableProxy = iLoggingEvent.getThrowableProxy();
            if (null != iThrowableProxy) {
                exceptionStorage.offer(new ClientException((String) ContextHolder.get("uri"), System.currentTimeMillis(), iThrowableProxy));
                latestRecordTimestamp = System.currentTimeMillis();
                this.addTrace(iThrowableProxy.getMessage());
            } else if (iLoggingEvent.getMessage() != null) {
                this.addTrace(iLoggingEvent.getMessage());
            }
        }
    }

    private void addTrace(String msg) {
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
//        }catch (Exception e){
//            //log.error(e.getMessage(),e);
//        }
    }

    @Override
    public boolean isEmpty() {
        return exceptionStorage.isEmpty();
    }

    @Override
    public List<?> buildAndGetThriftEntities(int interval, String appName, String ipAddress, int port) {
        return buildEntities(interval, appName, ipAddress, port);
    }

    private List<FailureMessageEntity> buildEntities(int interval, String appName, String ipAddress, int port) {
        List<FailureMessageDetailEntity> failureMessageDetailEntities = new ArrayList<>();
        ClientException clientException;
        long buildTimestamp = System.currentTimeMillis();
        int occurTimes = 0;
        Map<String, FailureMessageDetailEntity> entityMap = new HashMap<>();
        if (earliestRecordTimestamp > buildTimestamp || latestRecordTimestamp < buildTimestamp - (interval * 1000L)) {
            // no record in time_window, refresh the earliestTimestamp & latestTimestamp
            earliestRecordTimestamp = buildTimestamp;
            latestRecordTimestamp = buildTimestamp;
        } else {
            do {
                clientException = exceptionStorage.poll();
                if (null == clientException) {
                    break;
                }

                occurTimes++;
                IThrowableProxy t = clientException.getRootException();
                ExceptionEntity rootException = getExceptionEntityFromThrowable(t);
                //List<ExceptionEntity> causedByException = new LinkedList<>();
                //deepSearchCausedByExceptions(causedByException, t);


                FailureMessageDetailEntity entity = null;
                if ((entity = entityMap.get(clientException.getUri() + rootException.getExceptionId() + rootException.getMessageId())) == null) {
                    entity = new FailureMessageDetailEntity(
                        clientException.getTimestamp(),
                        rootException,
                        null
                    );
                    entity.setUrl(clientException.getUri());
                    entityMap.put(clientException.getUri() + rootException.getExceptionId() + rootException.getMessageId(), entity);
                    failureMessageDetailEntities.add(entity);
                }
                entity.setOccurTimes(entity.getOccurTimes() + 1);
            } while (clientException.timestamp < buildTimestamp);

            earliestRecordTimestamp = buildTimestamp;
        }

        return Collections.singletonList(new FailureMessageEntity(
            appName,
            ipAddress,
            port,
            failureMessageDetailEntities,
            occurTimes,
            null,
            ERROR_SOURCE_TYPE_LOGBACK
        ));
    }

    private ExceptionEntity getExceptionEntityFromThrowable(IThrowableProxy t) {
        String stack = getFullStack(t.getStackTraceElementProxyArray());
        ExceptionEntity e = new ExceptionEntity(
            t.getClassName(),
            t.getMessage(),
            (t.getStackTraceElementProxyArray() == null || t.getStackTraceElementProxyArray().length < 1) ? null : getFullStack(t.getStackTraceElementProxyArray())
        );
        e.setExceptionId(toHex(stack));
        e.setMessageId(toHex(t.getMessage()));
        return e;
    }

    private static String toHex(String stack) {
        if (stack == null) {
            return null;
        }
        try {
            byte[] byteArray = stack.getBytes(StandardCharsets.UTF_8);
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(byteArray);
            byte[] bytes = md5.digest();
            final char[] HEX_DIGITS = "0123456789ABCDEF".toCharArray();
            StringBuilder ret = new StringBuilder(bytes.length * 2);
            for (byte aByte : bytes) {
                ret.append(HEX_DIGITS[(aByte >> 4) & 0x0f]);
                ret.append(HEX_DIGITS[aByte & 0x0f]);
            }
            return ret.toString();
        } catch (Exception e) {

        }
        return null;
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

    private void deepSearchCausedByExceptions(List<ExceptionEntity> causedByException, IThrowableProxy t) {
        if (null != t.getCause()) {
            IThrowableProxy cause = t.getCause();
            causedByException.add(getExceptionEntityFromThrowable(cause));
            deepSearchCausedByExceptions(causedByException, cause);
        }
    }

}
