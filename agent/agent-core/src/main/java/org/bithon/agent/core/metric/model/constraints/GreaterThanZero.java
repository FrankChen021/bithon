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

package org.bithon.agent.core.metric.model.constraints;

import org.bithon.agent.core.metric.model.IMetricValueProvider;
import org.bithon.agent.core.metric.model.InvalidMetricValueException;

/**
 * @author frank.chen021@outlook.com
 * @date 2022/7/30 22:51
 */
public class GreaterThanZero implements IMetricValueProvider {
    private final IMetricValueProvider delegate;

    public GreaterThanZero(IMetricValueProvider delegate) {
        this.delegate = delegate;
    }

    @Override
    public long get() throws InvalidMetricValueException {
        long v = delegate.get();
        if (v > 0) {
            return v;
        } else {
            throw new InvalidMetricValueException();
        }
    }
}
