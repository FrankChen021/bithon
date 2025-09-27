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

package org.bithon.agent.observability.metric.model;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/16
 */
public class Min implements IMetricValueUpdater, IMetricValueProvider {
    private final AtomicLong value = new AtomicLong(Long.MAX_VALUE);

    @Override
    public long update(long value) {
        return this.value.accumulateAndGet(value, Math::min);
    }

    @Override
    public long get() {
        long value = this.value.getAndSet(Long.MAX_VALUE);
        return value == Long.MAX_VALUE ? 0 : value;
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
