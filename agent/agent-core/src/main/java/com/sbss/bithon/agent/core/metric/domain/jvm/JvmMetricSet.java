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

package com.sbss.bithon.agent.core.metric.domain.jvm;


/**
 * @author frank.chen021@outlook.com
 * @date 2020/10/27 2:17 下午
 */
public class JvmMetricSet {
    /**
     * uptime of the Java virtual machine in milliseconds.
     */
    public long upTime;

    /**
     * Returns the start time of the Java virtual machine in milliseconds.
     * This method returns the approximate time when the Java virtual machine started.
     */
    public long startTime;

    public CpuCompositeMetric cpuMetricSet;
    public MemoryCompositeMetric memoryMetricSet;
    public HeapCompositeMetric heapMetricSet;
    public NonHeapCompositeMetric nonHeapMetricSet;
    public ThreadCompositeMetric threadMetricSet;
    public ClassCompositeMetric classMetricSet;
    public MetaspaceCompositeMetric metaspaceMetricSet;

    public JvmMetricSet(long upTime, long startTime) {
        this.upTime = upTime;
        this.startTime = startTime;
    }
}
