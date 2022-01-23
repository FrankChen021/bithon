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

package org.bithon.agent.core.metric.model;

import java.util.function.LongSupplier;

/**
 * @author Frank Chen
 * @date 23/1/22 11:31 AM
 */
public class Gauge2 implements IMetricValueProvider {
    private LongSupplier delegate;

    public LongSupplier getDelegate() {
        return delegate;
    }

    public void setDelegate(LongSupplier delegate) {
        this.delegate = delegate;
    }

    @Override
    public long get() {
        return delegate == null ? 0 : delegate.getAsLong();
    }
}
