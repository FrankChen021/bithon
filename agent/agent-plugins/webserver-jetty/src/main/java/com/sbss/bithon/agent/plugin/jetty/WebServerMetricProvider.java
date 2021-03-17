package com.sbss.bithon.agent.plugin.jetty;

import com.sbss.bithon.agent.core.context.AppInstance;
import com.sbss.bithon.agent.core.dispatcher.IMessageConverter;
import com.sbss.bithon.agent.core.metrics.MetricProviderManager;
import com.sbss.bithon.agent.core.metrics.IMetricProvider;
import com.sbss.bithon.agent.core.metrics.web.WebServerMetric;
import com.sbss.bithon.agent.core.metrics.web.WebServerType;
import org.eclipse.jetty.server.AbstractNetworkConnector;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import java.util.Collections;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/14 9:30 下午
 */
public class WebServerMetricProvider implements IMetricProvider {

    private static final WebServerMetricProvider INSTANCE = new WebServerMetricProvider();

    public static WebServerMetricProvider getInstance() {
        return INSTANCE;
    }

    private AbstractNetworkConnector connector;
    private QueuedThreadPool threadPool;

    WebServerMetricProvider() {
        MetricProviderManager.getInstance().register("webserver-jetty", this);
    }

    @Override
    public boolean isEmpty() {
        return connector == null && threadPool == null;
    }

    @Override
    public List<Object> buildMessages(IMessageConverter messageConverter,
                                      AppInstance appInstance,
                                      int interval,
                                      long timestamp) {
        return Collections.singletonList(messageConverter.from(appInstance,
                                                               timestamp,
                                                               interval,
                                                               new WebServerMetric(
                                                                   WebServerType.JETTY,
                                                                   connector == null ? 0 : connector.getConnectedEndPoints().size(),
                                                                   connector == null ? 0 : connector.getAcceptors(),
                                                                   threadPool == null ? 0 : threadPool.getBusyThreads(),
                                                                   threadPool == null ? 0 : threadPool.getMaxThreads())));
    }

    public void setConnector(AbstractNetworkConnector connector) {
        this.connector = connector;
    }

    public void setThreadPool(QueuedThreadPool threadPool) {
        this.threadPool = threadPool;
    }
}
