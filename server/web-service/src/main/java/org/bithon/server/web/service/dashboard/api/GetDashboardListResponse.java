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

package org.bithon.server.web.service.dashboard.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bithon.server.storage.dashboard.Dashboard;

import java.util.List;

/**
 * Paginated result for dashboard list queries
 *
 * @author Frank Chen
 * @date 2025-09-11
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetDashboardListResponse {

    /**
     * List of dashboards in current page
     */
    private List<Dashboard> data;

    /**
     * Current page number (0-based)
     */
    private int page;

    /**
     * Page size
     */
    private int size;

    /**
     * Total number of elements across all pages
     */
    private long total;

    /**
     * Create a DashboardListResult with calculated pagination info
     */
    public static GetDashboardListResponse of(List<Dashboard> dashboards,
                                              int page,
                                              int size,
                                              long totalElements) {
        return GetDashboardListResponse.builder()
                                       .data(dashboards)
                                       .page(page)
                                       .size(size)
                                       .total(totalElements)
                                       .build();
    }
}
