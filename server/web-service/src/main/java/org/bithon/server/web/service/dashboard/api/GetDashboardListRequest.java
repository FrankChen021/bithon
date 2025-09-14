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

/**
 * Filter criteria for dashboard queries
 *
 * @author Frank Chen
 * @date 2025-09-11
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetDashboardListRequest {

    /**
     * Search term to match against title and folder (when folder is not specified)
     * or just title (when folder is specified)
     */
    private String search;

    /**
     * Exact folder path to filter by
     */
    private String folder;

    /**
     * Folder prefix to filter by (alternative to exact folder match)
     */
    private String folderPrefix;

    /**
     * Page number (0-based)
     */
    @Builder.Default
    private int page = 0;

    /**
     * Page size (max results per page)
     */
    @Builder.Default
    private int size = 100;

    /**
     * Sort field (title, folder, lastModified, name)
     */
    @Builder.Default
    private String sort = "title";

    /**
     * Sort order (asc, desc)
     */
    @Builder.Default
    private String order = "asc";
}
