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

package org.bithon.server.web.service.meta;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bithon.server.common.utils.EndPointType;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/6 12:05 下午
 */
@Builder
@Getter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class EndPointLink {
    private long timestamp;

    // dimension
    private EndPointType srcEndpointType;
    private String srcEndpoint;
    private EndPointType dstEndpointType;
    private String dstEndpoint;

    // metric
    private long interval;
    private long callCount;
    private long errorCount;
    private long responseTime;
    private long minResponseTime;
    private long maxResponseTime;

    public long getTimestamp() {
        return timestamp;
    }

    public List<Object> getDimensions() {
        return Arrays.asList(srcEndpointType, srcEndpoint, dstEndpointType, dstEndpoint);
    }

    public Map<String, Number> getMetrics() {
        return null;
    }
}
