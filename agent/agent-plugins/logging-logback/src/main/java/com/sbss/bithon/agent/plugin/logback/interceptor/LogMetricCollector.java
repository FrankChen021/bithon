/*
 *    Copyright 2020 bithon.cn
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

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
public class LogMetricCollector implements IMetricCollector {

    private final Queue<AppException> exceptionList = new ConcurrentLinkedDeque<>();

    public void addException(String uri, IThrowableProxy exception) {
        exceptionList.offer(new AppException(uri, exception));
    }

    @Override
    public boolean isEmpty() {
        return exceptionList.isEmpty();
    }

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
