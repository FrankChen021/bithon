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

package org.bithon.agent.sdk.metric.aggregator;

import org.bithon.agent.sdk.metric.IMetricValue;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author frank.chen021@outlook.com
 * @date 2021-10-01
 */
public class LongMax implements IMetricValue {
    private final AtomicLong value = new AtomicLong(Long.MIN_VALUE);

    private final long defaultValue;

    public LongMax() {
        this(0);
    }

    public LongMax(long defaultValue) {
        this.defaultValue = defaultValue;
    }

    @Override
    public long update(long value) {
        while (true) {
            long current = this.value.get();
            if (current < value) {
                if (this.value.compareAndSet(current, value)) {
                    return value;
                }
            } else {
                return current;
            }
        }
    }

    @Override
    public long value() {
        long v = value.get();
        return v == Long.MIN_VALUE ? this.defaultValue : v;
    }
}
