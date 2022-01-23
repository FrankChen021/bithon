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

import org.bithon.agent.core.metric.domain.web.WebServerMetricCollector;
import org.bithon.agent.core.metric.domain.web.WebServerMetrics;
import org.bithon.agent.core.metric.domain.web.WebServerType;
import org.eclipse.jetty.server.AbstractNetworkConnector;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import java.util.Collections;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/14 9:30 下午
 */
public class JettyWebServerMetricCollector extends WebServerMetricCollector {

    private static final JettyWebServerMetricCollector INSTANCE = new JettyWebServerMetricCollector();
    private AbstractNetworkConnector connector;
    private QueuedThreadPool threadPool;

    public JettyWebServerMetricCollector() {
        this.register(Collections.singletonList(WebServerType.JETTY.type()),
                      new WebServerMetrics(() -> connector == null ? 0 : connector.getConnectedEndPoints().size(),
                                           () -> connector == null ? 0 : connector.getAcceptors(),
                                           () -> threadPool == null ? 0 : threadPool.getBusyThreads(),
                                           () -> threadPool == null ? 0 : threadPool.getMaxThreads()));
    }

    public static JettyWebServerMetricCollector getInstance() {
        return INSTANCE;
    }

    public void setConnector(AbstractNetworkConnector connector) {
        this.connector = connector;
    }

    public void setThreadPool(QueuedThreadPool threadPool) {
        this.threadPool = threadPool;
    }
}
