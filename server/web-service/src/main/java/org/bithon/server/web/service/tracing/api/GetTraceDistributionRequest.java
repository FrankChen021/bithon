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
import org.bithon.server.storage.metrics.IFilter;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 17/12/21 4:06 PM
 */
@Data
public class GetTraceDistributionRequest {
    @NotBlank
    private String startTimeISO8601;

    @NotBlank
    private String endTimeISO8601;

    @NotNull
    private List<IFilter> filters = Collections.emptyList();

    /**
     * Filter expression
     */
    private String expression;

    @Min(30)
    @Max(60)
    private int bucketCount = 30;
}
