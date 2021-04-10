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

import com.sbss.bithon.agent.core.metric.domain.jvm.GcCompositeMetric;
import com.sbss.bithon.agent.core.metric.model.Delta;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/14 8:39 下午
 */
public class GcMetricCollector {

    static class GarbageCollector {
        private final GarbageCollectorMXBean gcBean;
        private final Delta gcCount = new Delta();
        private final Delta gcTime = new Delta();
        private final String generation;

        GarbageCollector(GarbageCollectorMXBean bean) {
            this.gcBean = bean;
            this.generation = getGeneration(bean.getName());
        }

        public GcCompositeMetric collect() {
            long gcCount = this.gcCount.update(gcBean.getCollectionCount());
            long gcTime = this.gcTime.update(gcBean.getCollectionTime());

            if (gcCount > 0 && gcTime > 0) {
                return new GcCompositeMetric(gcBean.getName(), generation, gcCount, gcTime);
            }
            return null;
        }

        private String getGeneration(String gcName) {
            switch (gcName) {
                // CMS
                case "ParNew":
                    return "new";
                case "ConcurrentMarkSweep":
                    return "old";

                // G1
                case "G1 Young Generation":
                    return "new";
                case "G1 Old Generation":
                    return "old";

                // Parallel
                case "PS Scavenge":
                    return "new";
                case "PS MarkSweep":
                    return "old";

                // Serial
                case "Copy":
                    return "new";
                case "MarkSweepCompact":
                    return "old";

                // unknown
                default:
                    return gcName;
            }
        }
    }

    private final Map<String, GarbageCollector> collectors = new HashMap<>();

    public GcMetricCollector() {
        for (GarbageCollectorMXBean gcBean : ManagementFactory.getGarbageCollectorMXBeans()) {
            collectors.put(gcBean.getName(), new GarbageCollector(gcBean));
        }
    }

    public List<GcCompositeMetric> collect() {
        List<GcCompositeMetric> metrics = new ArrayList<>(collectors.size());
        for (GarbageCollector gcBean : collectors.values()) {

            GcCompositeMetric metric = gcBean.collect();
            if (metric != null) {
                metrics.add(metric);
            }
        }
        return metrics;
    }

}
