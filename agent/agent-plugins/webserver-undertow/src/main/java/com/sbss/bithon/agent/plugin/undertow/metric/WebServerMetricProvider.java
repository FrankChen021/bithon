package com.sbss.bithon.agent.plugin.undertow.metric;

import com.sbss.bithon.agent.core.context.AppInstance;
import com.sbss.bithon.agent.core.dispatcher.IMessageConverter;
import com.sbss.bithon.agent.core.metric.IMetricProvider;
import com.sbss.bithon.agent.core.metric.MetricProviderManager;
import com.sbss.bithon.agent.core.metric.web.WebServerMetric;
import com.sbss.bithon.agent.core.metric.web.WebServerType;
import io.undertow.server.ConnectorStatistics;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/14 10:50 下午
 */
public class WebServerMetricProvider implements IMetricProvider {

    private static WebServerMetricProvider INSTANCE = new WebServerMetricProvider();

    public static WebServerMetricProvider getInstance() {
        return INSTANCE;
    }

    WebServerMetricProvider() {
        MetricProviderManager.getInstance().register("undertow-webserver", this);
    }

    private TaskPoolAccessor threadPoolAccessor;
    private ConnectorStatistics connectorStatistics;

    @Override
    public boolean isEmpty() {
        return connectorStatistics == null && threadPoolAccessor == null;
    }

    @Override
    public List<Object> buildMessages(IMessageConverter messageConverter,
                                      AppInstance appInstance,
                                      int interval,
                                      long timestamp) {

        long connectionCount = null == connectorStatistics ? 0 : connectorStatistics.getActiveConnections();

        //TODO: MaxActiveConnection is not maxConnections
        long maxConnections = null == connectorStatistics ? 0 : connectorStatistics.getMaxActiveConnections();

        return Collections.singletonList(messageConverter.from(appInstance,
                                                               timestamp,
                                                               interval,
                                                               new WebServerMetric(WebServerType.UNDERTOW,
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
            try {
                getActiveCount = this.taskPool.getClass().getDeclaredMethod("getActiveCount");
                getActiveCount.setAccessible(true);
                getMaximumPoolSize = this.taskPool.getClass().getDeclaredMethod("getMaximumPoolSize");
                getMaximumPoolSize.setAccessible(true);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }

        public int getActiveCount() {
            try {
                int activeCount = (int)getActiveCount.invoke(taskPool);
                return activeCount == -1 ? 0 : activeCount;
            } catch (IllegalAccessException | InvocationTargetException e) {
                //TODO: warning log
                return 0;
            }
        }

        public int getMaximumPoolSize() {
            try {
                return (int)getMaximumPoolSize.invoke(taskPool);
            } catch (IllegalAccessException | InvocationTargetException e) {
                //TODO: warning log
                return 0;
            }
        }
    }
}
