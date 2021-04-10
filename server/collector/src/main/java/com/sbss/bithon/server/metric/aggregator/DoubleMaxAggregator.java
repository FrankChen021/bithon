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

package com.sbss.bithon.server.metric.aggregator;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/4/6 9:32 下午
 */
public class DoubleMaxAggregator extends AbstractDoubleAggregator {

    public DoubleMaxAggregator() {
        this.value = Double.NEGATIVE_INFINITY;
    }

    @Override
    protected void aggregate(long timestamp, double value) {
        this.value = Math.max(this.value, value);
    }

    @Override
    public int intValue() {
        return (int) (value == Double.NEGATIVE_INFINITY ? 0 : value);
    }

    @Override
    public long longValue() {
        return (long) (value == Double.NEGATIVE_INFINITY ? 0 : value);
    }

    @Override
    public float floatValue() {
        return (float) (value == Double.NEGATIVE_INFINITY ? 0 : value);
    }

    @Override
    public double doubleValue() {
        return (value == Double.NEGATIVE_INFINITY ? 0 : value);
    }
}
