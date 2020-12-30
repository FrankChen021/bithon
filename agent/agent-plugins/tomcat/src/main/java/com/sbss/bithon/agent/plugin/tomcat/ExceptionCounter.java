package com.sbss.bithon.agent.plugin.tomcat;

import com.keruyun.commons.agent.collector.entity.ExceptionEntity;
import com.keruyun.commons.agent.collector.entity.FailureMessageDetailEntity;
import com.keruyun.commons.agent.collector.entity.FailureMessageEntity;
import com.sbss.bithon.agent.core.context.ContextHolder;
import com.sbss.bithon.agent.core.interceptor.AfterJoinPoint;
import com.sbss.bithon.agent.dispatcher.metrics.counter.IAgentCounter;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Description : 异常counter, 缓存发生的异常, 定期发送 <br>
 * Date: 18/2/5
 *
 * @author 马至远
 */
public class ExceptionCounter implements IAgentCounter {
    private static final int ERROR_SOURCE_TYPE_TOMCAT = 1;

    private long earliestRecordTimestamp = 0, latestRecordTimestamp = 0;

    private class ClientException {
        private long timestamp;
        private String uri;
        private Throwable rootException;

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }

        ClientException(long occurTimestamp, String uri, Throwable throwable) {
            this.timestamp = occurTimestamp;
            this.rootException = throwable;
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

    @Override
    public void add(Object o) {
        AfterJoinPoint afterJoinPoint = (AfterJoinPoint) o;

        Throwable e = (Throwable) afterJoinPoint.getArgs()[2];
        if (null != e) {
            exceptionStorage.offer(new ClientException(System.currentTimeMillis(),
                                                       (String) ContextHolder.get("uri"),
                                                       e));
            latestRecordTimestamp = System.currentTimeMillis();
        }
    }

    @Override
    public boolean isEmpty() {
        return exceptionStorage.isEmpty();
    }

    @Override
    public List buildAndGetThriftEntities(int interval,
                                          String appName,
                                          String ipAddress,
                                          int port) {
        return buildEntities(interval, appName, ipAddress, port);
    }

    @SuppressWarnings("Duplicates")
    private List<FailureMessageEntity> buildEntities(int interval,
                                                     String appName,
                                                     String ipAddress,
                                                     int port) {
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
                if ((entity = entityMap.get(clientException.getUri() + rootException.getExceptionId() +
                                                rootException.getMessageId())) == null) {
                    entity = new FailureMessageDetailEntity(clientException.getTimestamp(), rootException, null);
                    entity.setUrl(clientException.getUri());
                    entityMap.put(clientException.getUri() + rootException.getExceptionId() +
                                      rootException.getMessageId(),
                                  entity);
                    failureMessageDetailEntities.add(entity);
                }
            } while (clientException.timestamp < buildTimestamp);

            earliestRecordTimestamp = buildTimestamp;
        }

        return Collections.singletonList(new FailureMessageEntity(appName,
                                                                  ipAddress,
                                                                  port,
                                                                  failureMessageDetailEntities,
                                                                  occurTimes,
                                                                  null,
                                                                  ERROR_SOURCE_TYPE_TOMCAT));
    }

    private ExceptionEntity getExceptionEntityFromThrowable(Throwable t) {
        return new ExceptionEntity(t.getClass().getName(),
                                   t.getMessage(),
                                   (t.getStackTrace() == null ||
                                       t.getStackTrace().length < 1) ? null : t.getStackTrace()[0].toString());
    }

    private void deepSearchCausedByExceptions(List<ExceptionEntity> causedByException,
                                              Throwable t) {
        if (null != t.getCause()) {
            Throwable cause = t.getCause();
            causedByException.add(getExceptionEntityFromThrowable(cause));
            deepSearchCausedByExceptions(causedByException, cause);
        }
    }
}
