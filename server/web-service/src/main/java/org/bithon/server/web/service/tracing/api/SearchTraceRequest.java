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

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.bithon.server.storage.datasource.query.OrderBy;

import java.util.Collections;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 15/1/22 4:08 PM
 */
@Data
public class SearchTraceRequest {
    @NotBlank
    private String startTimeISO8601;

    @NotBlank
    private String endTimeISO8601;

    @NotEmpty
    private Map<String, String> conditions = Collections.emptyMap();

    private OrderBy orderBy;
    private int pageNumber = 0;
    private int pageSize = 10;
}
