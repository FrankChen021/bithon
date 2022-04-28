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

package org.bithon.server.alerting.common.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bithon.server.storage.metrics.IFilter;

import javax.validation.constraints.Size;
import java.util.List;

/**
 * @author frankchen
 * @date 2020-08-21 14:56:50
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertCondition {
    @JsonProperty
    @Size(min = 1, max = 1)
    private String id;

    @JsonProperty
    private String dataSource;

    @JsonProperty
    private List<IFilter> dimensions;

    @JsonProperty
    private IMetricCondition metric;
}
