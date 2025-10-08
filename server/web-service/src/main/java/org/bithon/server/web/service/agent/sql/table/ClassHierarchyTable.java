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
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Frank Chen
 * @date 29/9/25 2:35 pm
 */
public class ClassHierarchyTable extends AbstractBaseTable implements IPushdownPredicateProvider {

    private static final Pattern LAMBDA_PATTERN = Pattern.compile(".*\\$+Lambda\\$\\d+(/.*)?$");
    private static final Pattern HEX_PATTERN = Pattern.compile("0[xX][0-9a-fA-F]+|[0-9a-fA-F]+");

    public static class HierarchyEntry {
        public int id;
        public Integer parentId;
        public String className;
        public String tag;
        public boolean isLambda;

        public HierarchyEntry(int id, Integer parentId, String className, String tag, boolean isLambda) {
            this.id = id;
            this.parentId = parentId;
            this.className = className;
            this.tag = tag;
            this.isLambda = isLambda;
        }

        public Object[] toObjects() {
            return new Object[]{id, parentId, className, tag, isLambda};
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
        if (lines.length < 1) {
            return result;
        }

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

                    // Check if this is a lambda class
                    boolean isLambda = isLambdaClass(className);

                    HierarchyEntry entry = new HierarchyEntry(
                        currentId,
                        null, // Root has no parent
                        className,
                        tag,
                        isLambda
                    );

                    result.add(entry);
                    parentStack.add(currentId);
                    currentId++;
                }
                // Skip malformed lines that don't start with |-- and we already have a root
                continue;
            }

            // Parse the hierarchy prefix to determine depth and extract class name
            DepthAndIndex depthAndClassName = parseDepth(line);
            int depth = depthAndClassName.depth;
            String className = line.substring(depthAndClassName.index);
            
            // Parse tag if present (in parentheses) - do this first
            String tag = null;
            int parenIndex = className.lastIndexOf(" (");
            if (parenIndex >= 0) {
                tag = className.substring(parenIndex + 2, className.length() - 1);
                className = className.substring(0, parenIndex);
            }

            // Parse class name and extract suffix for tag
            // For lambda classes: MyClass$$Lambda$123/0x... - the /0x... part stays in class name
            // For regular classes: MyClass/0x... - the /0x... part is removed from class name
            int firstSlashIndex = className.indexOf('/');
            String suffix = null;
            if (firstSlashIndex >= 0) {
                String baseClassName = className.substring(0, firstSlashIndex);
                suffix = className.substring(firstSlashIndex + 1);
                
                // For lambda classes, keep the first part of suffix in class name (unless it's "null")
                if (LAMBDA_PATTERN.matcher(baseClassName).matches()) {
                    int secondSlashIndex = suffix.indexOf('/');
                    if (secondSlashIndex >= 0) {
                        // Lambda with multiple parts: keep first part in class name
                        className = baseClassName + "/" + suffix.substring(0, secondSlashIndex);
                    } else if (!"null".equals(suffix)) {
                        // Lambda with one part (not "null"): keep it in class name
                        className = baseClassName + "/" + suffix;
                    } else {
                        // Lambda with "/null": don't keep it in class name
                        className = baseClassName;
                    }
                } else {
                    // Regular class: remove suffix from class name
                    className = baseClassName;
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
            
            // Add formatted suffix (if present)
            if (suffix != null) {
                tagBuilder.append(formatTag(suffix));
            }
            
            // Add type annotation (if present)
            if (tag != null) {
                if (!tagBuilder.isEmpty()) {
                    tagBuilder.append(", ");
                }
                tagBuilder.append("type:").append(tag);
            }

            // Check if this is a lambda class
            boolean isLambda = isLambdaClass(className);

            // Create hierarchy entry
            HierarchyEntry entry = new HierarchyEntry(
                currentId,
                parentId,
                className,
                !tagBuilder.isEmpty() ? tagBuilder.toString() : null,
                isLambda
            );

            result.add(entry);

            // Update parent stack for next iteration
            parentStack.add(currentId);
            currentId++;
        }

        return result;
    }

    /**
     * Parse depth and class name from a hierarchy line.
     * Counts |-- or |  prefixes to determine depth, returns remaining part as class name.
     */
    private static class DepthAndIndex {
        final int depth;
        final int index;
        
        DepthAndIndex(int depth, int index) {
            this.depth = depth;
            this.index = index;
        }
    }
    
    private static DepthAndIndex parseDepth(String line) {
        int depth = 0;
        int index = 0;
        int length = line.length();
        
        // Count depth by checking |-- or |  prefixes
        while (index + 2 < length) {
            char c0 = line.charAt(index);
            char c1 = line.charAt(index + 1);
            char c2 = line.charAt(index + 2);
            
            // Check for |-- or |  (pipe + two spaces)
            if (c0 == '|' && ((c1 == '-' && c2 == '-') || (c1 == ' ' && c2 == ' '))) {
                depth++;
                index += 3;
            } else {
                break;
            }
        }
        
        return new DepthAndIndex(depth, index);
    }

    /**
     * Format tag from suffix: split by '/', add "hex:" prefix to hex values, "null" for null values,
     * and join with ", " in reverse order (rightmost first).
     */
    private static String formatTag(String suffix) {
        if (suffix == null || suffix.isEmpty()) {
            return null;
        }
        
        // Split by '/' and process in reverse order
        String[] parts = suffix.split("/");
        StringBuilder result = new StringBuilder();
        
        for (int i = parts.length - 1; i >= 0; i--) {
            String part = parts[i];
            if (result.length() > 0) {
                result.append(", ");
            }
            
            if ("null".equals(part)) {
                result.append("null");
            } else if (HEX_PATTERN.matcher(part).matches()) {
                result.append("hex:").append(part);
            } else {
                // Not a recognized format, just append as-is
                result.append(part);
            }
        }
        
        return result.length() > 0 ? result.toString() : null;
    }

    /**
     * Check if a class name represents a lambda class.
     * Lambda classes have names that contain $Lambda$ or $$Lambda$ followed by NUMBER where NUMBER is a string of digits.
     */
    private static boolean isLambdaClass(String className) {
        if (className == null) {
            return false;
        }
        // Match pattern: $Lambda$<digits> or $$Lambda$<digits>, optionally followed by /suffix
        // The (/.*)?$ part handles cases where the className still includes /hex or /null suffixes
        return LAMBDA_PATTERN.matcher(className).matches();
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
                !tagBuilder.isEmpty() ? tagBuilder.toString() : null,
                isLambdaClass(classInfo.name)
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
