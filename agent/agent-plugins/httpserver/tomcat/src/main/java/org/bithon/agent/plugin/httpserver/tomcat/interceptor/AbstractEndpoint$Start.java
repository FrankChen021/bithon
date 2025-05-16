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

package org.bithon.agent.plugin.httpserver.tomcat.interceptor;

import org.apache.tomcat.util.net.AbstractEndpoint;
import org.apache.tomcat.util.threads.ResizableExecutor;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AfterInterceptor;
import org.bithon.agent.observability.context.AppInstance;
import org.bithon.agent.observability.metric.collector.MetricRegistryFactory;
import org.bithon.agent.observability.metric.domain.httpserver.HttpServerMetricRegistry;
import org.bithon.agent.observability.metric.domain.httpserver.HttpServerMetrics;
import org.bithon.agent.observability.metric.domain.httpserver.HttpServerType;
import org.bithon.agent.observability.metric.model.schema.Dimensions;
import org.bithon.component.commons.utils.ReflectionUtils;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * {@link org.apache.tomcat.util.net.AbstractEndpoint#start()}
 *
 * @author frankchen
 */
@SuppressWarnings("rawtypes")
public class AbstractEndpoint$Start extends AfterInterceptor {

    private AbstractEndpoint endpoint;

    @Override
    public void after(AopContext context) {
        if (endpoint != null) {
            return;
        }

        endpoint = (AbstractEndpoint) context.getTarget();

        AppInstance.getInstance().setPort(endpoint.getPort());

        HttpServerMetrics metrics = MetricRegistryFactory.getOrCreateRegistry(HttpServerMetricRegistry.NAME, HttpServerMetricRegistry::new)
                                                         .getOrCreateMetrics(Dimensions.of(HttpServerType.UNDERTOW.type()),
                                                                             HttpServerMetrics::new);
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
