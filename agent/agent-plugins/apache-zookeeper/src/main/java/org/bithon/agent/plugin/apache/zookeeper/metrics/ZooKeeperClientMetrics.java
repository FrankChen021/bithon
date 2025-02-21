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

package org.bithon.agent.plugin.apache.zookeeper.metrics;

import org.bithon.agent.observability.metric.model.annotation.Max;
import org.bithon.agent.observability.metric.model.annotation.Min;
import org.bithon.agent.observability.metric.model.annotation.Sum;

/**
 * @author frank.chen021@outlook.com
 * @date 15/1/25 4:33 pm
 */
public class ZooKeeperClientMetrics {
    @Min
    public long minResponseTime;

    @Sum
    public long responseTime;

    @Max
    public long maxResponseTime;

    @Sum
    public long totalCount;

    @Sum
    public long bytesReceived;

    @Sum
    public long bytesSent;
}
