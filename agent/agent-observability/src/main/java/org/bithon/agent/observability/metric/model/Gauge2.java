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

import java.util.function.LongSupplier;

/**
 * @author frank.chen021@outlook.com
 * @date 23/1/22 11:31 AM
 */
public class Gauge2 implements IMetricValueProvider {
    private LongSupplier provider;

    public LongSupplier getProvider() {
        return provider;
    }

    public void setProvider(LongSupplier provider) {
        this.provider = provider;
    }

    @Override
    public long get() {
        return provider == null ? 0 : provider.getAsLong();
    }
}
