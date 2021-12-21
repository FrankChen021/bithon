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

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/6 4:17 下午
 */
@Data
public class GetTraceListRequest {
    @NotBlank
    private String application;

    @NotBlank
    private String startTimeISO8601;

    @NotBlank
    private String endTimeISO8601;

    private String order = "desc";
    private String orderBy = "";

    @Min(0)
    private int pageNumber = 0;

    @Min(5)
    @Max(100)
    private int pageSize = 10;
}
