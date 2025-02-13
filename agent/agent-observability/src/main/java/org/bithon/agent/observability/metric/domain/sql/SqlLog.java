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

package org.bithon.agent.observability.metric.domain.sql;

import org.bithon.agent.observability.metric.model.annotation.Max;
import org.bithon.agent.observability.metric.model.annotation.Min;
import org.bithon.agent.observability.metric.model.annotation.Sum;

/**
 * @author frank.chen021@outlook.com
 * @date 2025/2/5 20:19
 */
public class SqlLog {

    @Min
    public long minResponseTime;

    /**
     * nano seconds
     */
    @Sum
    public long responseTime;

    @Max
    public long maxResponseTime;

    @Max
    public long callCount;

    @Max
    public long bytesIn;

    @Max
    public long bytesOut;

}
