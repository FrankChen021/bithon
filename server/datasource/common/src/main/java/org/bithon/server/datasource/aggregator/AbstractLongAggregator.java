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

package org.bithon.server.datasource.aggregator;

import org.bithon.component.commons.utils.NumberUtils;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/4/9 19:24
 */
public abstract class AbstractLongAggregator extends NumberAggregator {

    protected long value;

    @Override
    public final void aggregate(long timestamp, Object value) {
        aggregate(timestamp, NumberUtils.getLong(value, 0));
    }

    @Override
    public Number getNumber() {
        return value;
    }

    protected abstract void aggregate(long timestamp, long value);

    @Override
    public int intValue() {
        return (int) value;
    }

    @Override
    public long longValue() {
        return value;
    }

    @Override
    public float floatValue() {
        return value;
    }

    @Override
    public double doubleValue() {
        return value;
    }
}
