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

package org.bithon.server.web.service.agent.sql.table;

import com.google.common.collect.ImmutableMap;
import org.bithon.agent.rpc.brpc.cmd.IJvmCommand;
import org.bithon.component.commons.exception.HttpMappableException;
import org.bithon.server.discovery.declaration.controller.IAgentControllerApi;
import org.bithon.server.web.service.common.calcite.SqlExecutionContext;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Frank Chen
 * @date 1/3/23 2:35 pm
 */
public class ClassHierarchyTable extends AbstractBaseTable implements IPushdownPredicateProvider {

    public static class HierarchyEntry {
        public int id;
        public Integer parentId;
        public String className;
        public String tag;

        public HierarchyEntry(int id, Integer parentId, String className, String tag) {
            this.id = id;
            this.parentId = parentId;
            this.className = className;
            this.tag = tag;
        }

        public Object[] toObjects() {
            return new Object[]{id, parentId, className, tag};
        }
    }

    private final AgentServiceProxyFactory proxyFactory;

    public ClassHierarchyTable(AgentServiceProxyFactory proxyFactory) {
        this.proxyFactory = proxyFactory;
    }

    @Override
    protected List<Object[]> getData(SqlExecutionContext executionContext) {
        try {
            String hierarchyText = proxyFactory.createBroadcastProxy(executionContext.getParameters(), IJvmCommand.class)
                                               .executeDiagnosticCommand("vmClassHierarchy", new String[]{});

            return parseClassHierarchy(hierarchyText).stream()
                                                     .map(HierarchyEntry::toObjects)
                                                     .collect(Collectors.toList());
        } catch (Exception e) {
            // Return empty list if histogram cannot be retrieved
            throw new HttpMappableException(e,
                                            HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                            "Failed to get class hierarchy: %s",
                                            e.getMessage());
        }
    }

    @Override
    protected Class<?> getRecordClazz() {
        return HierarchyEntry.class;
    }

    @Override
    public Map<String, Boolean> getPredicates() {
        return ImmutableMap.of(IAgentControllerApi.PARAMETER_NAME_APP_NAME, true,
                               IAgentControllerApi.PARAMETER_NAME_INSTANCE, true);
    }

    public static List<HierarchyEntry> parseClassHierarchy(String text) {
        List<HierarchyEntry> result = new ArrayList<>();

        if (text == null || text.trim().isEmpty()) {
            return result;
        }

        String[] lines = text.split("\\R");
        if (lines.length < 2) {
            return result;
        }

        // Skip the first line which contains the PID
        int currentId = 1;
        List<Integer> parentStack = new ArrayList<>();

        // No longer using regex - parsing manually for better control

        for (String s : lines) {
            String line = s.trim();
            if (line.isEmpty()) {
                continue;
            }

            // Check if this is a root class (no |-- or |  prefix)
            if (!line.startsWith("|--") && !line.startsWith("|  ")) {
                // This is the root class - only process if it's the first non-empty line after PID
                if (result.isEmpty()) {
                    String className = line;
                    String tag = null;

                    // Check if there's a /null suffix
                    if (className.endsWith("/null")) {
                        className = className.substring(0, className.length() - 5);
                        tag = "null";
                    }

                    HierarchyEntry entry = new HierarchyEntry(
                        currentId,
                        null, // Root has no parent
                        className,
                        tag
                    );

                    result.add(entry);
                    parentStack.add(currentId);
                    currentId++;
                }
                // Skip malformed lines that don't start with |-- and we already have a root
                continue;
            }

            // Parse the hierarchy prefix manually
            int depth = 0;
            String className = line;
            String hexNumber = null;
            String tag = null;

            // Count |-- sequences for old format or |  sequences for new format
            if (line.startsWith("|  ")) {
                // New format: |  |  |--className
                while (className.startsWith("|  ")) {
                    depth++;
                    className = className.substring(3);
                }
                if (className.startsWith("|--")) {
                    depth++;
                    className = className.substring(3);
                }
            } else if (line.startsWith("|--")) {
                // Old format: |--|--|--className
                while (className.startsWith("|--")) {
                    depth++;
                    className = className.substring(3);
                }
            }

            // Parse tag if present (in parentheses) - do this first
            if (className.contains(" (")) {
                int parenIndex = className.lastIndexOf(" (");
                tag = className.substring(parenIndex + 2, className.length() - 1);
                className = className.substring(0, parenIndex);
            }

            // Parse hex number if present
            if (className.contains("/")) {
                int slashIndex = className.lastIndexOf("/");
                String potentialHex = className.substring(slashIndex + 1);
                // Check if it's a valid hex number (starts with 0x, 0X, or is all hex digits)
                if (potentialHex.matches("0x[0-9a-fA-F]+|0X[0-9a-fA-F]+|[0-9a-fA-FxX]+")) {
                    hexNumber = potentialHex;
                    className = className.substring(0, slashIndex);
                }
            }

            className = className.trim();

            // Adjust parent stack to match current depth
            while (parentStack.size() > depth) {
                parentStack.remove(parentStack.size() - 1);
            }

            // Determine parent ID
            Integer parentId = null;
            if (depth > 0 && !parentStack.isEmpty()) {
                parentId = parentStack.get(parentStack.size() - 1);
            }

            // Build tag information
            StringBuilder tagBuilder = new StringBuilder();
            if (hexNumber != null) {
                tagBuilder.append("hex:").append(hexNumber);
            }
            if (tag != null) {
                if (!tagBuilder.isEmpty()) {
                    tagBuilder.append(", ");
                }
                tagBuilder.append("type:").append(tag);
            }

            // Create hierarchy entry
            HierarchyEntry entry = new HierarchyEntry(
                currentId,
                parentId,
                className,
                !tagBuilder.isEmpty() ? tagBuilder.toString() : null
            );

            result.add(entry);

            // Update parent stack for next iteration
            parentStack.add(currentId);
            currentId++;
        }

        return result;
    }

    /**
     * Build a basic hierarchy from the loaded class list.
     * This is a fallback when VM.class_hierarchy command is not available.
     */
    private List<HierarchyEntry> buildBasicHierarchy(List<IJvmCommand.ClassInfo> classList) {
        List<HierarchyEntry> result = new ArrayList<>();

        if (classList == null || classList.isEmpty()) {
            return result;
        }

        // Create a map of class names to their hierarchy entries
        Map<String, HierarchyEntry> classMap = new HashMap<>();
        int currentId = 1;

        // First pass: create entries for all classes
        for (IJvmCommand.ClassInfo classInfo : classList) {
            if (classInfo.name == null || classInfo.name.isEmpty()) {
                continue;
            }

            // Build tag information
            StringBuilder tagBuilder = new StringBuilder();
            if (classInfo.isInterface) {
                tagBuilder.append("type:interface");
            } else if (classInfo.isAnnotation) {
                tagBuilder.append("type:annotation");
            } else if (classInfo.isEnum) {
                tagBuilder.append("type:enum");
            } else if (classInfo.isArray) {
                tagBuilder.append("type:array");
            } else if (classInfo.isPrimitive) {
                tagBuilder.append("type:primitive");
            } else if (classInfo.isSynthetic) {
                tagBuilder.append("type:synthetic");
            }

            HierarchyEntry entry = new HierarchyEntry(
                currentId,
                null, // Will be set in second pass
                classInfo.name,
                !tagBuilder.isEmpty() ? tagBuilder.toString() : null
            );

            result.add(entry);
            classMap.put(classInfo.name, entry);
            currentId++;
        }

        // Second pass: establish parent-child relationships
        for (IJvmCommand.ClassInfo classInfo : classList) {
            if (classInfo.name == null || classInfo.name.isEmpty()) {
                continue;
            }

            HierarchyEntry entry = classMap.get(classInfo.name);
            if (entry == null) {
                continue;
            }

            // Try to find parent class
            try {
                Class<?> clazz = Class.forName(classInfo.name);
                Class<?> superClass = clazz.getSuperclass();

                if (superClass != null && !superClass.equals(Object.class)) {
                    HierarchyEntry parentEntry = classMap.get(superClass.getName());
                    if (parentEntry != null) {
                        entry.parentId = parentEntry.id;
                    }
                }
            } catch (Exception e) {
                // Ignore classes that can't be loaded
                // This can happen with anonymous classes, lambda classes, etc.
            }
        }

        return result;
    }
}
