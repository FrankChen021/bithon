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

package org.bithon.server.web.service.datasource.api;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bithon.component.commons.utils.HumanReadableDuration;
import org.bithon.server.commons.time.TimeSpan;

import javax.annotation.Nullable;

/**
 * @author Frank Chen
 * @date 22/3/22 3:27 PM
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntervalRequest {
    @NotBlank
    private String startISO8601;

    @NotBlank
    private String endISO8601;

    /**
     * An expression that allows the client to change the default timestamp column,
     * so that some functions can be used upon the timestamp column before aggregation.
     * This helps if the underlying datasource provides PROJECTION support upon the timestamp column.
     */
    @Nullable
    private String timestampColumn;

    /**
     * The count of time buckets if the query is going to group the result by a time interval.
     * In most cases, users don't need to set this value.
     * If it's null, it defaults to {@link org.bithon.server.web.service.common.bucket.TimeBucket#calculate(TimeSpan, TimeSpan)}
     */
    @Nullable
    private Integer bucketCount;

    /**
     * The step (in seconds) between two adjacent data points.
     * The {@param bucketCount} and this parameter cannot be specified at the same time.
     * And this parameter has higher priority than {@param bucketCount}
     */
    private Integer step;

    private HumanReadableDuration window;
}
