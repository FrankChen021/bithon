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

package org.bithon.agent.core.metric.domain.web;

import org.bithon.agent.core.metric.model.Gauge;
import org.bithon.agent.core.metric.model.IMetricSet;
import org.bithon.agent.core.metric.model.IMetricValueProvider;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/13 10:51 下午
 */
public class WebServerMetrics implements IMetricSet {

    public final Gauge connectionCount;
    public final Gauge maxConnections;
    public final Gauge activeThreads;
    public final Gauge maxThreads;
    public final IMetricValueProvider[] metrics;

    public WebServerMetrics(Gauge connectionCount,
                            Gauge maxConnections,
                            Gauge activeThreads,
                            Gauge maxThreads) {
        this.connectionCount = connectionCount;
        this.maxConnections = maxConnections;
        this.activeThreads = activeThreads;
        this.maxThreads = maxThreads;
        metrics = new IMetricValueProvider[]{
            connectionCount,
            maxConnections,
            activeThreads,
            maxThreads
        };
    }

    @Override
    public IMetricValueProvider[] getMetrics() {
        return metrics;
    }
}
