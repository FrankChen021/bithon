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

package org.bithon.server.web.service.topo.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/20 21:46
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Link {
    private String srcEndpoint;
    private String dstEndpoint;

    private double minResponseTime;
    private double maxResponseTime;
    private double avgResponseTime;
    private long callCount;
    private long errorCount;
    private double qps;
}
