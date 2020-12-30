package com.sbss.bithon.agent.plugin.log4j2;

import com.keruyun.commons.agent.collector.entity.ExceptionEntity;
import com.keruyun.commons.agent.collector.entity.FailureMessageDetailEntity;
import com.keruyun.commons.agent.collector.entity.FailureMessageEntity;
import com.sbss.bithon.agent.core.context.ContextHolder;
import com.sbss.bithon.agent.core.interceptor.AfterJoinPoint;
import com.sbss.bithon.agent.dispatcher.metrics.counter.IAgentCounter;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.spi.StandardLevel;

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
    private static final int ERROR_SOURCE_TYPE_LOG4J2 = 4;
    private static final String KEY = "log4j2";
    private long earliestRecordTimestamp = 0, latestRecordTimestamp = 0;

    private class ClientException {
        private long timestamp;
        private String uri;
        private Throwable rootException;

        ClientException(String uri, long occurTimestamp, Throwable throwable) {
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

        Throwable getRootException() {
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

        Level logLevel = (Level) afterJoinPoint.getArgs()[1];
        if (StandardLevel.ERROR.equals(logLevel.getStandardLevel())) {
            Throwable e = (Throwable) afterJoinPoint.getArgs()[4];
            if (null != e) {
                exceptionStorage.offer(new ClientException((String) ContextHolder.get("uri"), System.currentTimeMillis(), e));
                latestRecordTimestamp = System.currentTimeMillis();
                this.addTrace(e.getMessage());
            } else {
                Message msg = (Message) afterJoinPoint.getArgs()[3];
                if (msg != null) {
                    exceptionStorage.offer(new ClientException((String) ContextHolder.get("uri"), System.currentTimeMillis(), new RuntimeException(msg.getFormattedMessage())));
                    this.addTrace(msg.getFormattedMessage());
                }
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
//        }catch (Exception ex){
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
        if (earliestRecordTimestamp > buildTimestamp || latestRecordTimestamp < buildTimestamp - (interval * 1000)) {
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
                Throwable t = clientException.getRootException();
                ExceptionEntity rootException = getExceptionEntityFromThrowable(t);
                // List<ExceptionEntity> causedByException = new LinkedList<>();
                // deepSearchCausedByExceptions(causedByException, t);

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
            ERROR_SOURCE_TYPE_LOG4J2
        ));
    }

    private ExceptionEntity getExceptionEntityFromThrowable(Throwable t) {
        String stack = getFullStack(t.getStackTrace());
        ExceptionEntity e = new ExceptionEntity(
            t.getClass().getName(),
            t.getMessage(),
            (t.getStackTrace() == null || t.getStackTrace().length < 1) ? null : getFullStack(t.getStackTrace())
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
            byte[] byteArray = stack.getBytes("utf-8");
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(byteArray);
            byte[] bytes = md5.digest();
            final char[] HEX_DIGITS = "0123456789ABCDEF".toCharArray();
            StringBuilder ret = new StringBuilder(bytes.length * 2);
            for (int i = 0; i < bytes.length; i++) {
                ret.append(HEX_DIGITS[(bytes[i] >> 4) & 0x0f]);
                ret.append(HEX_DIGITS[bytes[i] & 0x0f]);
            }
            return ret.toString();
        } catch (Exception e) {

        }
        return null;
    }

    private String getFullStack(StackTraceElement[] stacks) {
        StringBuffer sb = new StringBuffer();
        if (stacks != null && stacks.length > 0) {
            for (StackTraceElement msg : stacks) {
                sb.append(msg.toString() + "\r\n");
            }
        }
        return sb.toString();
    }

    private void deepSearchCausedByExceptions(List<ExceptionEntity> causedByException, Throwable t) {
        if (null != t.getCause()) {
            Throwable cause = t.getCause();
            causedByException.add(getExceptionEntityFromThrowable(cause));
            deepSearchCausedByExceptions(causedByException, cause);
        }
    }

}
