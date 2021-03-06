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

package org.bithon.agent.plugin.jetty.interceptor;

import org.bithon.agent.bootstrap.aop.AbstractInterceptor;
import org.bithon.agent.bootstrap.aop.AopContext;
import org.bithon.agent.core.metric.collector.MetricRegistryFactory;
import org.bithon.agent.core.metric.domain.web.WebServerMetricRegistry;
import org.bithon.agent.core.metric.domain.web.WebServerMetrics;
import org.bithon.agent.core.metric.domain.web.WebServerType;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import java.util.Collections;

/**
 * @author frankchen
 */
public class QueuedThreadPoolDoStart extends AbstractInterceptor {

    @Override
    public void onMethodLeave(AopContext context) {
        QueuedThreadPool threadPool = context.castTargetAs();

        WebServerMetrics metrics = MetricRegistryFactory.getOrCreateRegistry(WebServerMetricRegistry.NAME, WebServerMetricRegistry::new)
                                                        .getOrCreateMetrics(Collections.singletonList(WebServerType.JETTY.type()),
                                                                            WebServerMetrics::new);

        metrics.activeThreads.setProvider(threadPool::getBusyThreads);
        metrics.maxThreads.setProvider(threadPool::getMaxThreads);
    }
}
