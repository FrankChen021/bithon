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

package org.bithon.agent.observability.metric.domain.jvm;

import org.bithon.agent.observability.metric.model.Delta;
import org.bithon.agent.observability.metric.model.IMetricSet;
import org.bithon.agent.observability.metric.model.IMetricValueProvider;
import org.bithon.agent.observability.metric.model.constraints.GreaterThanZero;

import java.lang.management.GarbageCollectorMXBean;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/12/29 9:58 下午
 */
public class GcMetrics implements IMetricSet {
    /**
     * count of GC between two intervals
     */
    private final IMetricValueProvider gcCount;

    /**
     * time of total GC between two intervals in milli seconds
     */
    private final IMetricValueProvider gcTime;

    public GcMetrics(GarbageCollectorMXBean bean) {
        gcCount = new GreaterThanZero(new IMetricValueProvider() {
            private final Delta delta = new Delta(bean.getCollectionCount());

            @Override
            public long get() {
                return delta.update(bean.getCollectionCount());
            }
        });

        gcTime = new GreaterThanZero(new IMetricValueProvider() {

            private final Delta delta = new Delta(bean.getCollectionTime());

            @Override
            public long get() {
                return delta.update(bean.getCollectionTime());
            }
        });
    }

    @Override
    public IMetricValueProvider[] getMetrics() {
        return new IMetricValueProvider[]{
            gcCount, gcTime
        };
    }
}
