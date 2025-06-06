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

package org.bithon.agent.plugin.httpserver.jetty.interceptor;

import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AfterInterceptor;
import org.bithon.agent.observability.metric.collector.MetricRegistryFactory;
import org.bithon.agent.observability.metric.domain.httpserver.HttpServerMetricRegistry;
import org.bithon.agent.observability.metric.domain.httpserver.HttpServerMetrics;
import org.bithon.agent.observability.metric.domain.httpserver.HttpServerType;
import org.bithon.agent.observability.metric.model.schema.Dimensions;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

/**
 * @author frankchen
 */
public class QueuedThreadPool$DoStart extends AfterInterceptor {

    @Override
    public void after(AopContext context) {
        QueuedThreadPool threadPool = context.getTargetAs();

        HttpServerMetrics metrics = MetricRegistryFactory.getOrCreateRegistry(HttpServerMetricRegistry.NAME, HttpServerMetricRegistry::new)
                                                         .getOrCreateMetrics(Dimensions.of(HttpServerType.JETTY.type()),
                                                                             HttpServerMetrics::new);

        metrics.queueSize.setProvider(threadPool::getQueueSize);
        metrics.pooledThreads.setProvider(threadPool::getThreads);
        metrics.activeThreads.setProvider(threadPool::getBusyThreads);
        metrics.maxThreads.setProvider(threadPool::getMaxThreads);
    }
}
