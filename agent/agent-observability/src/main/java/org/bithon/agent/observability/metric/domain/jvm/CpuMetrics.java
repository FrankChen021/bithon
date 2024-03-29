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

package org.bithon.agent.observability.metric.domain.jvm;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/12/29 9:49 下午
 */
public class CpuMetrics {
    public long processorNumber;

    // CPU Time in nano seconds
    public long processCpuTime;

    public double avgSystemLoad;

    // CPU usage (%)
    public double processCpuLoad;

    public CpuMetrics(long processorNumber,
                      long processCpuTime,
                      double avgSystemLoad,
                      double processCpuLoad) {
        this.processorNumber = processorNumber;
        this.processCpuTime = processCpuTime;
        this.avgSystemLoad = avgSystemLoad;
        this.processCpuLoad = processCpuLoad;
    }
}
