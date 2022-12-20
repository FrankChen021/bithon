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

package org.bithon.agent.plugin.tomcat.interceptor;

import org.apache.tomcat.util.net.AbstractEndpoint;
import org.apache.tomcat.util.threads.ResizableExecutor;
import org.bithon.agent.bootstrap.aop.AbstractInterceptor;
import org.bithon.agent.bootstrap.aop.AopContext;
import org.bithon.agent.core.context.AgentContext;
import org.bithon.agent.core.metric.collector.MetricRegistryFactory;
import org.bithon.agent.core.metric.domain.web.WebServerMetricRegistry;
import org.bithon.agent.core.metric.domain.web.WebServerMetrics;
import org.bithon.agent.core.metric.domain.web.WebServerType;
import org.bithon.component.commons.utils.ReflectionUtils;

import java.util.Collections;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author frankchen
 */
public class AbstractEndpointStart extends AbstractInterceptor {

    private AbstractEndpoint<?> endpoint;

    @Override
    public void onMethodLeave(AopContext context) {
        if (endpoint != null) {
            return;
        }

        endpoint = (AbstractEndpoint<?>) context.getTarget();

        AgentContext.getInstance().getAppInstance().setPort(endpoint.getPort());

        WebServerMetrics metrics = MetricRegistryFactory.getOrCreateRegistry(WebServerMetricRegistry.NAME, WebServerMetricRegistry::new)
                                                        .getOrCreateMetrics(Collections.singletonList(WebServerType.UNDERTOW.type()),
                                                                            WebServerMetrics::new);
        metrics.connectionCount.setProvider(endpoint::getConnectionCount);
        metrics.maxConnections.setProvider(endpoint::getMaxConnections);
        metrics.activeThreads.setProvider(endpoint::getCurrentThreadsBusy);
        metrics.pooledThreads.setProvider(endpoint::getCurrentThreadCount);
        metrics.maxThreads.setProvider(endpoint::getMaxThreads);
        metrics.queueSize.setProvider(() -> {
            Executor e = endpoint.getExecutor();
            if (e instanceof org.apache.tomcat.util.threads.ThreadPoolExecutor) {
                return ((org.apache.tomcat.util.threads.ThreadPoolExecutor) e).getQueue().size();
            } else if (e instanceof ThreadPoolExecutor) {
                return ((ThreadPoolExecutor) e).getQueue().size();
            } else if (e instanceof ResizableExecutor) {
                org.apache.tomcat.util.threads.ThreadPoolExecutor t = (org.apache.tomcat.util.threads.ThreadPoolExecutor) ReflectionUtils.getFieldValue(e,
                                                                                                                                                        "executor");
                return t.getQueue().size();
            }
            return -1;
        });
    }
}
