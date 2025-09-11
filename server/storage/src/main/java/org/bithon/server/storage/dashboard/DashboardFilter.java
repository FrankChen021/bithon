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

package org.bithon.server.storage.dashboard;

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
public class DashboardFilter {

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

    /**
     * Include deleted dashboards
     */
    @Builder.Default
    private boolean includeDeleted = false;

    /**
     * Maximum page size allowed
     */
    public static final int MAX_PAGE_SIZE = 1000;

    /**
     * Get validated page size (ensure it's within limits)
     */
    public int getValidatedSize() {
        return Math.min(Math.max(1, size), MAX_PAGE_SIZE);
    }

    /**
     * Get validated page number (ensure it's not negative)
     */
    public int getValidatedPage() {
        return Math.max(0, page);
    }

    /**
     * Check if search term is provided and meaningful
     */
    public boolean hasSearch() {
        return search != null && search.trim().length() >= 2;
    }

    /**
     * Check if folder filter is provided
     */
    public boolean hasFolder() {
        return folder != null && !folder.trim().isEmpty();
    }

    /**
     * Check if folder prefix filter is provided
     */
    public boolean hasFolderPrefix() {
        return folderPrefix != null && !folderPrefix.trim().isEmpty();
    }

    /**
     * Get trimmed search term
     */
    public String getTrimmedSearch() {
        return search != null ? search.trim() : null;
    }

    /**
     * Get trimmed folder
     */
    public String getTrimmedFolder() {
        return folder != null ? folder.trim() : null;
    }

    /**
     * Get trimmed folder prefix
     */
    public String getTrimmedFolderPrefix() {
        return folderPrefix != null ? folderPrefix.trim() : null;
    }
}
