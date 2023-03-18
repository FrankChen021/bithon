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

import org.bithon.agent.observability.metric.collector.MetricRegistry;
import org.bithon.agent.observability.metric.domain.jvm.GcMetrics;

import java.util.Arrays;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/14 8:39 下午
 */
public class GcMetricRegistry extends MetricRegistry<GcMetrics> {

    public static String getGeneration(String gcName) {
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

    public GcMetricRegistry() {
        super("jvm-gc-metrics",
              Arrays.asList("gcName", "generation"),
              GcMetrics.class,
              null,
              false);
    }
}
