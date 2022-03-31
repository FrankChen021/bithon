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
import org.bithon.server.storage.datasource.api.IQuerableAggregator;
import org.bithon.server.storage.metrics.IFilter;
import org.bithon.server.storage.metrics.OrderBy;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.util.Collections;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2022/11/01 14:28
 */
@Data
public class GroupByQueryRequest {
    @NotEmpty
    private String startTimeISO8601;

    @NotEmpty
    private String endTimeISO8601;

    @NotEmpty
    private String dataSource;

    private List<IFilter> filters = Collections.emptyList();

    private List<String> metrics = Collections.emptyList();
    private List<IQuerableAggregator> aggregators = Collections.emptyList();

    @Valid
    @Size(min = 1)
    private List<String> groupBy;

    private OrderBy orderBy;
}
