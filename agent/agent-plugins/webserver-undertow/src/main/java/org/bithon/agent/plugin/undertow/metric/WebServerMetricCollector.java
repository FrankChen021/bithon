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

package org.bithon.agent.plugin.undertow.metric;

import io.undertow.server.ConnectorStatistics;
import org.bithon.agent.bootstrap.expt.AgentException;
import org.bithon.agent.core.dispatcher.IMessageConverter;
import org.bithon.agent.core.metric.collector.IMetricCollector;
import org.bithon.agent.core.metric.collector.MetricCollectorManager;
import org.bithon.agent.core.metric.domain.web.WebServerMetrics;
import org.bithon.agent.core.metric.domain.web.WebServerType;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/14 10:50 下午
 */
public class WebServerMetricCollector implements IMetricCollector {

    private static final WebServerMetricCollector INSTANCE = new WebServerMetricCollector();

    public static WebServerMetricCollector getInstance() {
        return INSTANCE;
    }

    WebServerMetricCollector() {
        MetricCollectorManager.getInstance().register("undertow-webserver", this);
    }

    private TaskPoolAccessor threadPoolAccessor;
    private ConnectorStatistics connectorStatistics;

    @Override
    public boolean isEmpty() {
        return connectorStatistics == null && threadPoolAccessor == null;
    }

    @Override
    public List<Object> collect(IMessageConverter messageConverter,
                                int interval,
                                long timestamp) {

        long connectionCount = null == connectorStatistics ? 0 : connectorStatistics.getActiveConnections();

        //TODO: MaxActiveConnection is not maxConnections
        long maxConnections = null == connectorStatistics ? 0 : connectorStatistics.getMaxActiveConnections();

        return Collections.singletonList(messageConverter.from(timestamp,
                                                               interval,
                                                               new WebServerMetrics(WebServerType.UNDERTOW,
                                                                                    connectionCount,
                                                                                    maxConnections,
                                                                                    threadPoolAccessor.getActiveCount(),
                                                                                    threadPoolAccessor.getMaximumPoolSize())));
    }

    public void setThreadPool(Object webRequestThreadPool) {
        this.threadPoolAccessor = new TaskPoolAccessor(webRequestThreadPool);
    }

    public void setConnectorStatistics(ConnectorStatistics connectorStatistics) {
        this.connectorStatistics = connectorStatistics;
    }

    /**
     * Implementations of TaskPool in different version(3.3.8 used by Undertow 1.x vs 3.8 used by Undertow 2.x) differ from each other
     * Fortunately, they have same method name so that we could use reflect to unify the code together
     */
    static class TaskPoolAccessor {
        private final Object taskPool;
        private final Method getActiveCount;
        private final Method getMaximumPoolSize;

        TaskPoolAccessor(Object taskPool) {
            this.taskPool = taskPool;
            getActiveCount = getMethod(this.taskPool.getClass(), "getActiveCount");
            getActiveCount.setAccessible(true);
            getMaximumPoolSize = getMethod(this.taskPool.getClass(), "getMaximumPoolSize");
            getMaximumPoolSize.setAccessible(true);
        }

        Method getMethod(Class<?> clazz, String name) {
            while (clazz != null) {
                try {
                    return clazz.getDeclaredMethod(name);
                } catch (NoSuchMethodException e) {
                    clazz = clazz.getSuperclass();
                }
            }
            throw new AgentException("can't find [%s] in [%s]", name, clazz.getName());
        }

        public int getActiveCount() {
            try {
                int activeCount = (int) getActiveCount.invoke(taskPool);
                return activeCount == -1 ? 0 : activeCount;
            } catch (IllegalAccessException | InvocationTargetException e) {
                //TODO: warning log
                return 0;
            }
        }

        public int getMaximumPoolSize() {
            try {
                return (int) getMaximumPoolSize.invoke(taskPool);
            } catch (IllegalAccessException | InvocationTargetException e) {
                //TODO: warning log
                return 0;
            }
        }
    }
}
