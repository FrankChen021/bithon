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

package org.bithon.agent.core.metric.model;

/**
 * It's a compound metric which holds total time, min time and max time
 *
 * @author frank.chen021@outlook.com
 * @date 2021-03-16
 */
public class Timer implements ISimpleMetric {

    private final Sum sum = new Sum();
    private final Max max = new Max();
    private final Min min = new Min();

    @Override
    public long update(long value) {
        max.update(value);
        min.update(value);
        return sum.update(value);
    }

    public Sum getSum() {
        return sum;
    }

    public Max getMax() {
        return max;
    }

    public Min getMin() {
        return min;
    }
}
