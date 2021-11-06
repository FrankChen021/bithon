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

package org.bithon.server.topo.service;

import org.bithon.server.common.utils.datetime.TimeSpan;
import org.springframework.stereotype.Service;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/20 21:41
 */
@Service
public class TopoService {

    public Topo getCallee(TimeSpan startTime,
                          TimeSpan endTime,
                          String application,
                          int calleeDepth) {
        return null;
    }

    public Topo getCaller(String startTimeISO8601,
                          String endTimeISO8601,
                          String application,
                          @Min(1) @Max(5) int callerDepth) {
        return null;
    }
}
