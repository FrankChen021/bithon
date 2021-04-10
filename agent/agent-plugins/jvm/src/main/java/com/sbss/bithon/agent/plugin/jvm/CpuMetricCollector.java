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

package com.sbss.bithon.agent.plugin.jvm;

import com.sbss.bithon.agent.core.metric.domain.jvm.CpuCompositeMetric;
import com.sbss.bithon.agent.core.metric.model.Delta;

import java.util.concurrent.TimeUnit;

import static com.sbss.bithon.agent.plugin.jvm.JmxBeans.OS_BEAN;
import static com.sbss.bithon.agent.plugin.jvm.JmxBeans.RUNTIME_BEAN;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/14 8:33 下午
 */
public class CpuMetricCollector {
    /**
     * in NANO-seconds
     */
    private final Delta processCpuTime = new Delta(OS_BEAN.getProcessCpuTime());

    /**
     * in milli-seconds
     */
    private final Delta processUpTime = new Delta(RUNTIME_BEAN.getUptime());

    /**
     * Calculate CPU usage by code instead of by JMX API
     * operatingSystemMXBean.getProcessCpuLoad()
     * https://bugs.java.com/bugdatabase/view_bug.do?bug_id=JDK-8226575
     * https://github.com/alibaba/Sentinel/pull/1204
     */
    public CpuCompositeMetric collect() {
        long processCpuTimeDelta = processCpuTime.update(OS_BEAN.getProcessCpuTime());
        long processUpTimeDelta = processUpTime.update(RUNTIME_BEAN.getUptime());
        int cpuCores = OS_BEAN.getAvailableProcessors();

        double processCpuPercentage = (double) TimeUnit.NANOSECONDS.toMillis(processCpuTimeDelta)
                                      / processUpTimeDelta
                                      / cpuCores * 100;

        return new CpuCompositeMetric(cpuCores,
                                      processCpuTimeDelta,
                                      OS_BEAN.getSystemLoadAverage(),
                                      processCpuPercentage);
    }
}
