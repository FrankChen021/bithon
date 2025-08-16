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

package org.bithon.agent.observability.tracing.reporter;

import org.bithon.agent.configuration.annotation.ConfigurationProperties;
import org.bithon.agent.configuration.annotation.PropertyDescriptor;

/**
 * @author frank.chen021@outlook.com
 * @date 2025/8/6 21:10
 */
@ConfigurationProperties(path = "tracing.reporter")
public class ReporterConfig {
    /**
     * How many spans should be batched per tracing context.
     * This controls the maximum number of spans that can be stored in a single tracing context
     * so that the memory usage can be controlled if a tracing context has a long lifetime.
     * <p>
     * This provides a balance between memory usage and performance.
     */
    @PropertyDescriptor(
        value = "The maximum number of spans that can be batched per tracing context. The larger the number, the more memory will be used by the tracing context. " +
                "However, it can improve performance by reducing the number of times the tracing context is flushed to the reporter.",
        required = false
    )
    private int batchSize = 64;

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }
}
