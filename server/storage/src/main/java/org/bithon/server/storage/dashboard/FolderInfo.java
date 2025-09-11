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

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Folder information for dashboard organization
 * 
 * @author Frank Chen
 * @date 2025-09-11
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FolderInfo {
    
    /**
     * Full folder path (e.g., "observability/apps")
     */
    private String path;
    
    /**
     * Folder name (e.g., "apps" for path "observability/apps")
     */
    private String name;
    
    /**
     * Number of dashboards directly in this folder
     */
    private long dashboardCount;
    
    /**
     * Child folders
     */
    private List<FolderInfo> children;
    
    /**
     * Parent folder path (null for root folders)
     */
    private String parentPath;
    
    /**
     * Depth level (0 for root folders)
     */
    private int depth;
}
