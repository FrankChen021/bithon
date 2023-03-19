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

package org.bithon.agent.plugin.undertow.interceptor;

import io.undertow.UndertowOptions;
import io.undertow.server.ConnectorStatistics;
import io.undertow.server.protocol.http.HttpOpenListener;
import org.bithon.agent.bootstrap.aop.AopContext;
import org.bithon.agent.bootstrap.aop.interceptor.AfterInterceptor;
import org.bithon.agent.observability.metric.collector.MetricRegistryFactory;
import org.bithon.agent.observability.metric.domain.web.WebServerMetricRegistry;
import org.bithon.agent.observability.metric.domain.web.WebServerMetrics;
import org.bithon.agent.observability.metric.domain.web.WebServerType;
import org.xnio.OptionMap;

import java.util.Collections;

/**
 * @author frankchen
 */
public class HttpOpenListenerSetRootHandler extends AfterInterceptor {

    @Override
    public void after(AopContext aopContext) {
        HttpOpenListener openListener = aopContext.getTargetAs();
        openListener.setUndertowOptions(OptionMap.builder().addAll(openListener.getUndertowOptions())
                                                 .set(UndertowOptions.ENABLE_CONNECTOR_STATISTICS, true)
                                                 .getMap());

        WebServerMetrics metrics = MetricRegistryFactory.getOrCreateRegistry(WebServerMetricRegistry.NAME, WebServerMetricRegistry::new)
                                                        .getOrCreateMetrics(Collections.singletonList(WebServerType.UNDERTOW.type()),
                                                                            WebServerMetrics::new);

        ConnectorStatistics connectorStatistics = openListener.getConnectorStatistics();
        metrics.connectionCount.setProvider(connectorStatistics::getActiveConnections);
        metrics.maxConnections.setProvider(connectorStatistics::getMaxActiveConnections);
    }
}
