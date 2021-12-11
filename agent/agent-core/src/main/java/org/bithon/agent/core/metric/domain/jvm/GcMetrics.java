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

package org.bithon.agent.core.metric.domain.jvm;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/12/29 9:58 下午
 */
public class GcMetrics {
    private String gcName;

    /**
     * 0 - NEW
     * 1 - OLD
     */
    private String generation;

    /**
     * count of GC between two intervals
     */
    private long gcCount;

    /**
     * time of total GC between two intervals in milli seconds
     */
    private long gcTime;

    public String getGcName() {
        return gcName;
    }

    public String getGeneration() {
        return generation;
    }

    public long getGcCount() {
        return gcCount;
    }

    public long getGcTime() {
        return gcTime;
    }

    public GcMetrics(String gcName, String generation, long gcCount, long gcTime) {
        this.gcName = gcName;
        this.generation = generation;
        this.gcCount = gcCount;
        this.gcTime = gcTime;
    }
}
