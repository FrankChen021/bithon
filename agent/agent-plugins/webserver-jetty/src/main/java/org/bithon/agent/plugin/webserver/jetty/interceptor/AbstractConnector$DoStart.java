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

package org.bithon.agent.plugin.webserver.jetty.interceptor;

import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AfterInterceptor;
import org.bithon.agent.observability.context.AppInstance;
import org.bithon.agent.observability.metric.collector.MetricRegistryFactory;
import org.bithon.agent.observability.metric.domain.web.WebServerMetricRegistry;
import org.bithon.agent.observability.metric.domain.web.WebServerMetrics;
import org.bithon.agent.observability.metric.domain.web.WebServerType;
import org.eclipse.jetty.server.AbstractNetworkConnector;

import java.util.Collections;

/**
 * {@link org.eclipse.jetty.server.AbstractConnector#doStart()}
 *
 * @author frankchen
 */
public class AbstractConnector$DoStart extends AfterInterceptor {

    @Override
    public void after(AopContext context) {
        AbstractNetworkConnector connector = (AbstractNetworkConnector) context.getTarget();

        // notify to start emit the metrics
        AppInstance.getInstance().setPort(connector.getPort());

        WebServerMetrics metrics = MetricRegistryFactory.getOrCreateRegistry(WebServerMetricRegistry.NAME, WebServerMetricRegistry::new)
                                                        .getOrCreateMetrics(Collections.singletonList(WebServerType.JETTY.type()),
                                                                            WebServerMetrics::new);

        metrics.connectionCount.setProvider(() -> connector.getConnectedEndPoints().size());
        metrics.maxConnections.setProvider(connector::getAcceptors);
    }
}
