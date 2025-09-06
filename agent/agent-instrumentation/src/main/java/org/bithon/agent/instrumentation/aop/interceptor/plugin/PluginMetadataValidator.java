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

package org.bithon.agent.instrumentation.aop.interceptor.plugin;

import java.io.File;

/**
 * Utility class to validate that a plugin's generated metadata file can be correctly loaded
 * by the PluginMetadata.Loader. This is used during each plugin's build process to ensure
 * the generated metadata file is valid before packaging.
 *
 * @author frank.chen021@outlook.com
 * @date 2025/09/06
 */
public class PluginMetadataValidator {

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: PluginMetadataValidator directory-of-meta-files");
            System.exit(1);
        }

        String metaDirPath = args[0];
        File metaDir = new File(metaDirPath);

        if (!metaDir.exists()) {
            System.out.println("No plugin metadata directory found: " + metaDirPath + " (skipping validation)");
            System.exit(0);
        }

        if (!metaDir.isDirectory()) {
            System.err.println("Error: Path is not a directory: " + metaDirPath);
            System.exit(1);
        }

        // Check for merged plugins.meta file first
        File mergedMetaFile = new File(metaDir, "plugins.meta");
        if (mergedMetaFile.exists()) {
            System.out.println("Found merged plugins.meta file, validating: " + mergedMetaFile.getAbsolutePath());
            try {
                PluginMetadata metadata = PluginMetadata.Loader.load(mergedMetaFile);
                validateMetadata(metadata, mergedMetaFile.getName());

                System.out.println("✓ Merged plugin metadata validation successful!");
                System.out.println("  - Found " + metadata.pluginInfoList.size() + " plugin(s)");
                System.out.println("  - Found " + metadata.interceptorTypes.size() + " interceptor type mapping(s)");

                return;
            } catch (Exception e) {
                System.err.println("Error: Failed to load or validate merged plugin metadata file: " + e.getMessage());
                e.printStackTrace();
                System.exit(1);
            }
        }

        // Look for individual .meta files in META-INF/bithon subdirectory
        File individualMetaDir = new File(metaDir, "META-INF/bithon");
        if (individualMetaDir.exists() && individualMetaDir.isDirectory()) {
            metaDir = individualMetaDir;
            metaDirPath = individualMetaDir.getAbsolutePath();
        }

        // Find all .meta files in the directory
        File[] metaFiles = metaDir.listFiles((dir, name) -> name.endsWith(".meta"));

        if (metaFiles == null || metaFiles.length == 0) {
            System.out.println("No plugin metadata files found in: " + metaDirPath + " (skipping validation)");
            System.exit(0);
        }

        try {
            System.out.println("Validating plugin metadata files in: " + metaDirPath);

            for (File metaFile : metaFiles) {
                System.out.println("Validating: " + metaFile.getName());

                // Load the metadata file using the same loader that will be used at runtime
                PluginMetadata metadata = PluginMetadata.Loader.load(metaFile);

                // Validate the loaded metadata
                validateMetadata(metadata, metaFile.getName());

                System.out.println("✓ Plugin metadata validation successful for: " + metaFile.getName());
                System.out.println("  - Found " + metadata.pluginInfoList.size() + " plugin(s)");
                System.out.println("  - Found " + metadata.interceptorTypes.size() + " interceptor type mapping(s)");

                // Print details of the plugin
                for (PluginMetadata.PluginInfo pluginInfo : metadata.pluginInfoList) {
                    System.out.println("  - Plugin: " + pluginInfo.className + " (minimalJdkVersion=" + pluginInfo.minimalJdkVersion + ")");
                }
            }

            System.out.println("✓ All plugin metadata files validated successfully!");

        } catch (Exception e) {
            System.err.println("Error: Failed to load or validate plugin metadata files: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Validates the loaded metadata to ensure it contains valid data for a single plugin
     */
    private static void validateMetadata(PluginMetadata metadata, String fileName) {
        if (metadata == null) {
            throw new IllegalStateException("Loaded metadata is null");
        }

        if (metadata.pluginInfoList == null) {
            throw new IllegalStateException("Plugin info list is null");
        }

        if (metadata.interceptorTypes == null || metadata.interceptorTypes.isEmpty()) {
            throw new IllegalStateException("Interceptor types map is null/empty");
        }

        // For individual plugin validation, we expect exactly one plugin
        if (metadata.pluginInfoList.isEmpty()) {
            throw new IllegalStateException("No plugins found in metadata file");
        }

        // Validate the plugin info
        for (PluginMetadata.PluginInfo pluginInfo : metadata.pluginInfoList) {
            if (pluginInfo.className == null || pluginInfo.className.trim().isEmpty()) {
                throw new IllegalStateException("Plugin class name is null or empty");
            }

            if (pluginInfo.minimalJdkVersion <= 0) {
                throw new IllegalStateException("Invalid minimal JDK version for plugin " + pluginInfo.className + ": " + pluginInfo.minimalJdkVersion);
            }
        }
    }
}
