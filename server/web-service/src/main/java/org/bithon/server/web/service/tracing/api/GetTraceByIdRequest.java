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

package org.bithon.server.web.service.tracing.api;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/6 3:55 下午
 */
@Data
public class GetTraceByIdRequest {
    /**
     * a trace id or a user transaction id
     */
    @NotEmpty
    private String id;

    /**
     * trace - the value of id field is a trace id
     * auto  - the value of id field may be a user transaction id or a trace id
     */
    private String type = "trace";

    private String startTimeISO8601;
    private String endTimeISO8601;

    private boolean asTree = false;
}
