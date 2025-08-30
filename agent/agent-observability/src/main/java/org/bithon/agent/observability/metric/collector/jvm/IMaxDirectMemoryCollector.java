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

package org.bithon.agent.observability.metric.collector.jvm;

/**
 * Interface for collecting maximum direct memory information across different JDK versions.
 * This interface abstracts the JDK version-specific implementations to avoid reflection
 * and module access issues in JDK 9+.
 *
 * @author frank.chen021@outlook.com
 * @date 2024/12/19
 */
public interface IMaxDirectMemoryCollector {

    /**
     * Gets the maximum amount of direct memory that can be allocated.
     *
     * @return the maximum direct memory in bytes, or -1 if the maximum is unlimited
     */
    long getMaxDirectMemory();
}

