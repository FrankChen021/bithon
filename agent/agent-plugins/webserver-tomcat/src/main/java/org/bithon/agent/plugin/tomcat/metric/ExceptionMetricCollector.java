/*
 *    Copyright 2020 bithon.org
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

package org.bithon.agent.plugin.tomcat.metric;

import org.bithon.agent.core.context.InterceptorContext;
import org.bithon.agent.core.dispatcher.IMessageConverter;
import org.bithon.agent.core.metric.collector.IMetricCollector;

import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * @author frankchen
 */
public class ExceptionMetricCollector implements IMetricCollector {

    private final Queue<ExceptionWrapper> exceptionStorage = new ConcurrentLinkedDeque<>();

    public void update(Throwable e) {
        if (null != e) {
            exceptionStorage.offer(new ExceptionWrapper(System.currentTimeMillis(),
                                                        (String) InterceptorContext.get("uri"),
                                                        e));
        }
    }

    @Override
    public boolean isEmpty() {
        return exceptionStorage.isEmpty();
    }

    @Override
    public List<Object> collect(IMessageConverter messageConverter,
                                int interval,
                                long timestamp) {
        //TODO: use event to dispatch
        exceptionStorage.clear();
        return Collections.emptyList();
    }

    private static class ExceptionWrapper {
        private final long timestamp;
        private final Throwable rootException;
        private String uri;

        ExceptionWrapper(long occurTimestamp, String uri, Throwable throwable) {
            this.timestamp = occurTimestamp;
            this.rootException = throwable;
            this.uri = uri;
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

            ExceptionWrapper that = (ExceptionWrapper) o;

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
}
