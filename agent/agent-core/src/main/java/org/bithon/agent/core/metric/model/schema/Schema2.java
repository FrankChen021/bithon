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

package org.bithon.agent.core.metric.model.schema;

import java.util.List;

/**
 * @author Frank Chen
 * @date 2021-10-02
 */
public class Schema2 {
    private final String name;
    private final List<String> dimensionsSpec;
    private final List<String> metricsSpec;

    public Schema2(String name,
                   List<String> dimensionsSpec,
                   List<String> metricsSpec) {
        this.name = name;
        this.dimensionsSpec = dimensionsSpec;
        this.metricsSpec = metricsSpec;
    }

    public String getName() {
        return name;
    }

    public List<String> getDimensionsSpec() {
        return dimensionsSpec;
    }

    public List<String> getMetricsSpec() {
        return metricsSpec;
    }
}
