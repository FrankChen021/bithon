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

package org.bithon.agent.plugin.jetty.metric;

import org.bithon.agent.core.dispatcher.IMessageConverter;
import org.bithon.agent.core.metric.collector.IMetricCollector;
import org.bithon.agent.core.metric.collector.MetricCollectorManager;
import org.bithon.agent.core.metric.domain.web.WebServerMetrics;
import org.bithon.agent.core.metric.domain.web.WebServerType;
import org.eclipse.jetty.server.AbstractNetworkConnector;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import java.util.Collections;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/14 9:30 下午
 */
public class WebServerMetricCollector implements IMetricCollector {

    private static final WebServerMetricCollector INSTANCE = new WebServerMetricCollector();
    private AbstractNetworkConnector connector;
    private QueuedThreadPool threadPool;

    WebServerMetricCollector() {
        MetricCollectorManager.getInstance().register("jetty-webserver-metrics", this);
    }

    public static WebServerMetricCollector getInstance() {
        return INSTANCE;
    }

    @Override
    public boolean isEmpty() {
        return connector == null && threadPool == null;
    }

    @Override
    public List<Object> collect(IMessageConverter messageConverter,
                                int interval,
                                long timestamp) {
        return Collections.singletonList(messageConverter.from(timestamp,
                                                               interval,
                                                               new WebServerMetrics(
                                                                   WebServerType.JETTY,
                                                                   connector == null
                                                                   ? 0
                                                                   : connector.getConnectedEndPoints().size(),
                                                                   connector == null ? 0 : connector.getAcceptors(),
                                                                   threadPool == null ? 0 : threadPool.getBusyThreads(),
                                                                   threadPool == null
                                                                   ? 0
                                                                   : threadPool.getMaxThreads())));
    }

    public void setConnector(AbstractNetworkConnector connector) {
        this.connector = connector;
    }

    public void setThreadPool(QueuedThreadPool threadPool) {
        this.threadPool = threadPool;
    }
}
