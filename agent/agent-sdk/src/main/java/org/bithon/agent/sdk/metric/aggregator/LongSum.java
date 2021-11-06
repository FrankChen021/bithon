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
 * @author Frank Chen
 * @date 2021-10-01
 */
public class LongSum implements IMetricValue {
    private final AtomicLong value = new AtomicLong();

    @Override
    public long update(long value) {
        return this.value.addAndGet(value);
    }

    @Override
    public long value() {
        return this.value.get();
    }

    public void incr() {
        this.value.incrementAndGet();
    }
}
