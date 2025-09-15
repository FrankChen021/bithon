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

package org.bithon.server.storage;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for StorageModuleAutoConfiguration
 */
class StorageModuleAutoConfigurationTest {

    @Test
    void testExtractFolderFromResourcePath_RootLevel() throws Exception {
        // Test file directly in dashboard directory
        URI uri = new URI("classpath:/dashboard/application-overview.json");
        String result = StorageModuleAutoConfiguration.extractFolderFromResourcePath(uri);
        assertEquals("", result, "Root level files should return empty string");
    }

    @Test
    void testExtractFolderFromResourcePath_SingleLevel() throws Exception {
        // Test file in single subdirectory
        URI uri = new URI("classpath:/dashboard/metrics/jvm-metrics.json");
        String result = StorageModuleAutoConfiguration.extractFolderFromResourcePath(uri);
        assertEquals("metrics", result, "Single level subdirectory should return folder name");
    }

    @Test
    void testExtractFolderFromResourcePath_MultipleLevels() throws Exception {
        // Test file in nested subdirectories
        URI uri = new URI("classpath:/dashboard/apps/backend/performance-metrics.json");
        String result = StorageModuleAutoConfiguration.extractFolderFromResourcePath(uri);
        assertEquals("apps/backend", result, "Multiple level subdirectories should return full path");
    }

    @Test
    void testExtractFolderFromResourcePath_DeepNesting() throws Exception {
        // Test file in deeply nested subdirectories
        URI uri = new URI("classpath:/dashboard/category/subcategory/type/specific-dashboard.json");
        String result = StorageModuleAutoConfiguration.extractFolderFromResourcePath(uri);
        assertEquals("category/subcategory/type", result, "Deep nesting should return full directory path");
    }

    @Test
    void testExtractFolderFromResourcePath_SpecialCharacters() throws Exception {
        // Test file with special characters in directory names
        URI uri = new URI("classpath:/dashboard/test-metrics_v2/dashboard-1.0.json");
        String result = StorageModuleAutoConfiguration.extractFolderFromResourcePath(uri);
        assertEquals("test-metrics_v2", result, "Directory names with special characters should be preserved");
    }

    @Test
    void testExtractFolderFromResourcePath_JarFile() throws Exception {
        // Test file from jar/war resource
        URI uri = new URI("jar:file:/path/to/app.jar!/BOOT-INF/classes!/dashboard/metrics/system-metrics.json");
        String result = StorageModuleAutoConfiguration.extractFolderFromResourcePath(uri);
        assertEquals("metrics", result, "Jar file resources should extract folder correctly");
    }

    @Test
    void testExtractFolderFromResourcePath_FileProtocol() throws Exception {
        // Test file protocol URI
        URI uri = new URI("file:/Users/test/project/target/classes/dashboard/alerts/system-alerts.json");
        String result = StorageModuleAutoConfiguration.extractFolderFromResourcePath(uri);
        assertEquals("alerts", result, "File protocol URIs should extract folder correctly");
    }

    @Test
    void testExtractFolderFromResourcePath_NoDashboardPrefix() throws Exception {
        // Test URI without dashboard prefix
        URI uri = new URI("classpath:/config/some-config.json");
        String result = StorageModuleAutoConfiguration.extractFolderFromResourcePath(uri);
        assertEquals("", result, "URIs without dashboard prefix should return empty string");
    }

    @Test
    void testExtractFolderFromResourcePath_EmptyPath() throws Exception {
        // Test edge case with empty path after dashboard
        URI uri = new URI("classpath:/dashboard/");
        String result = StorageModuleAutoConfiguration.extractFolderFromResourcePath(uri);
        assertEquals("", result, "Empty path after dashboard should return empty string");
    }

    @Test
    void testExtractFolderFromResourcePath_OnlyDashboardPrefix() throws Exception {
        // Test URI with only dashboard prefix
        URI uri = new URI("classpath:/dashboard");
        String result = StorageModuleAutoConfiguration.extractFolderFromResourcePath(uri);
        assertEquals("", result, "URI with only dashboard prefix should return empty string");
    }
}
