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

package org.bithon.server.storage.datasource.spec;

import org.bithon.server.storage.datasource.spec.gauge.GaugeMetricSpec;
import org.bithon.server.storage.datasource.spec.max.MaxMetricSpec;
import org.bithon.server.storage.datasource.spec.min.MinMetricSpec;
import org.bithon.server.storage.datasource.spec.sum.SumMetricSpec;

/**
 * An adpator which helps simplify the implementation of some sub-classes without declaring all methods
 *
 * @author frank.chen021@outlook.com
 * @date 2022/10/30 12:52
 */
public class MetricSpecVisitorAdaptor<T> implements IMetricSpecVisitor<T> {
    @Override
    public T visit(SumMetricSpec metricSpec) {
        return null;
    }

    @Override
    public T visit(GaugeMetricSpec metricSpec) {
        return null;
    }

    @Override
    public T visit(CountMetricSpec metricSpec) {
        return null;
    }

    @Override
    public T visit(PostAggregatorMetricSpec metricSpec) {
        return null;
    }

    @Override
    public T visit(MinMetricSpec metricSpec) {
        return null;
    }

    @Override
    public T visit(MaxMetricSpec metricSpec) {
        return null;
    }
}
