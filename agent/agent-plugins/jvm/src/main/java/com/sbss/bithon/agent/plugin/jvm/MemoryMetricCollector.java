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

import com.sbss.bithon.agent.core.metric.domain.jvm.HeapCompositeMetric;
import com.sbss.bithon.agent.core.metric.domain.jvm.MemoryCompositeMetric;
import com.sbss.bithon.agent.core.metric.domain.jvm.MetaspaceCompositeMetric;
import com.sbss.bithon.agent.core.metric.domain.jvm.NonHeapCompositeMetric;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/14 8:21 下午
 */
public class MemoryMetricCollector {

    public static MemoryCompositeMetric buildMemoryMetrics() {
        return new MemoryCompositeMetric(Runtime.getRuntime().totalMemory(),
                                         Runtime.getRuntime().freeMemory());

    }

    public static HeapCompositeMetric collectHeap() {
        return new HeapCompositeMetric(JmxBeans.MEM_BEAN.getHeapMemoryUsage().getMax(),
                                       JmxBeans.MEM_BEAN.getHeapMemoryUsage().getInit(),
                                       JmxBeans.MEM_BEAN.getHeapMemoryUsage().getUsed(),
                                       JmxBeans.MEM_BEAN.getHeapMemoryUsage().getCommitted());

    }

    public static NonHeapCompositeMetric collectNonHeap() {
        return new NonHeapCompositeMetric(JmxBeans.MEM_BEAN.getNonHeapMemoryUsage().getMax(),
                                          JmxBeans.MEM_BEAN.getNonHeapMemoryUsage().getInit(),
                                          JmxBeans.MEM_BEAN.getNonHeapMemoryUsage().getUsed(),
                                          JmxBeans.MEM_BEAN.getNonHeapMemoryUsage()
                                                           .getCommitted());
    }

    public static MetaspaceCompositeMetric collectMeataSpace() {
        MetaspaceCompositeMetric metrics = new MetaspaceCompositeMetric();
        for (MemoryPoolMXBean bean : ManagementFactory.getMemoryPoolMXBeans()) {
            if ("Metaspace".equalsIgnoreCase(bean.getName())) {
                metrics.metaspaceCommittedBytes = bean.getUsage().getCommitted();
                metrics.metaspaceUsedBytes = bean.getUsage().getUsed();
            }
        }
        return metrics;
    }
}
