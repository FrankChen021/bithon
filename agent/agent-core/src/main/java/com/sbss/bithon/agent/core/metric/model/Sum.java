/*
 *    Copyright 2020 bithon.cn
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

package com.sbss.bithon.agent.core.metric.model;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents a cumulative value and its value will be flushed after be accessed
 *
 * @author frank.chen021@outlook.com
 * @date 2021/2/23 9:18 下午
 */
public class Sum implements ISimpleMetric {
    private final AtomicLong value;

    public Sum() {
        this(0L);
    }

    public Sum(long initialValue) {
        value = new AtomicLong(initialValue);
    }

    public void incr() {
        value.incrementAndGet();
    }

    @Override
    public long update(long delta) {
        if (delta != 0) {
            return value.addAndGet(delta);
        }
        return value.get();
    }

    public long get() {
        return value.getAndSet(0);
    }

    public long peek() {
        return value.get();
    }
}
