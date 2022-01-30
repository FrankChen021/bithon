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
import org.bithon.server.metric.storage.DimensionCondition;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/30 8:20 下午
 */
@Data
public class GetMetricsRequest {
    @NotEmpty
    private String startTimeISO8601;

    @NotEmpty
    private String endTimeISO8601;

    @NotEmpty
    private String dataSource;

    @Deprecated
    @Valid
    @Size(min = 1)
    private Map<String, DimensionCondition> dimensions;

    private List<DimensionCondition> filters = Collections.emptyList();

    @Size(min = 1)
    private List<String> metrics;

    private List<String> groups = Collections.emptyList();
}
