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

package org.bithon.server.web.service.api;

import lombok.Data;
import org.bithon.server.metric.storage.IFilter;
import org.bithon.server.metric.storage.OrderBy;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.Collections;
import java.util.List;

/**
 * @author Frank Chen
 * @date 2/2/22 11:20 AM
 */
@Data
public class ListQueryRequest {
    @NotBlank
    private String startTimeISO8601;

    @NotBlank
    private String endTimeISO8601;

    @NotBlank
    private String dataSource;

    private List<IFilter> filters = Collections.emptyList();

    @Size(min = 1)
    private List<String> columns;

    private OrderBy orderBy;

    private int pageNumber = 0;
    private int pageSize = 10;
}
