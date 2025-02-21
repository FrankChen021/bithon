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

import org.bithon.agent.observability.metric.model.schema.Dimensions;

/**
 * @author frank.chen021@outlook.com
 * @date 3/10/21 11:40
 */
public interface IMeasurement {

    long getTimestamp();

    Dimensions getDimensions();

    int getMetricCount();

    long getMetricValue(int index);

    /**
     * Mainly for testing purpose
     */
    default long getMetricValue(String name) {
        throw new UnsupportedOperationException();
    }
}
