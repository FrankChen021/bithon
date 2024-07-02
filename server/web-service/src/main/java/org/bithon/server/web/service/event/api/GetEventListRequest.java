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

package org.bithon.server.web.service.event.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.bithon.server.storage.datasource.filter.IColumnFilter;

import java.util.Collections;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 22/12/21 11:19 AM
 */
@Data
public class GetEventListRequest {
    
    @NotBlank
    private String startTimeISO8601;

    @NotBlank
    private String endTimeISO8601;

    private List<IColumnFilter> filters = Collections.emptyList();

    @Min(0)
    private int pageNumber = 0;

    @Min(0)
    @Max(100)
    private int pageSize = 10;
}
