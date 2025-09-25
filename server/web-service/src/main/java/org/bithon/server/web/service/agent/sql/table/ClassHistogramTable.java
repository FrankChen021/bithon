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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Frank Chen
 * @date 24/9/25 8:18 pm
 */
public class ClassHistogramTable extends AbstractBaseTable implements IPushdownPredicateProvider {

    public static class HistogramEntry {
        public int num;
        public long instances;
        public long bytes;
        public String clazz;
        public String module;
        public boolean isArray;

        public HistogramEntry(int num, long instances, long bytes, String clazz, String module, boolean isArray) {
            this.num = num;
            this.instances = instances;
            this.bytes = bytes;
            this.clazz = clazz;
            this.module = module;
            this.isArray = isArray;
        }

        public Object[] toObjects() {
            return new Object[]{num, instances, bytes, clazz, module, isArray};
        }
    }

    private final AgentServiceProxyFactory proxyFactory;

    public ClassHistogramTable(AgentServiceProxyFactory proxyFactory) {
        this.proxyFactory = proxyFactory;
    }

    @Override
    protected List<Object[]> getData(SqlExecutionContext executionContext) {
        try {
            String histogramText = proxyFactory.createBroadcastProxy(executionContext.getParameters(), IJvmCommand.class)
                                               .dumpClassHistogram(false);

            return parseHistogram(histogramText).stream()
                                                .map(HistogramEntry::toObjects)
                                                .collect(Collectors.toList());
        } catch (Exception e) {
            // Return empty list if histogram cannot be retrieved
            throw new HttpMappableException(e,
                                            HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                            "Failed to get class histogram: %s",
                                            e.getMessage());
        }
    }

    @Override
    protected Class<?> getRecordClazz() {
        return HistogramEntry.class;
    }

    @Override
    public Map<String, Boolean> getPredicates() {
        return ImmutableMap.of(IAgentControllerApi.PARAMETER_NAME_APP_NAME, true,
                               IAgentControllerApi.PARAMETER_NAME_INSTANCE, true);
    }

    public static List<HistogramEntry> parseHistogram(String text) {
        List<HistogramEntry> result = new ArrayList<>();

        // Regex: matches lines like "   1:      38804    14000080  [B (java.base@17.0.12)"
        Pattern p = Pattern.compile(
            "\\s*(\\d+):\\s+(\\d+)\\s+(\\d+)\\s+(.+)"
        );

        for (String line : text.split("\\R")) {
            Matcher m = p.matcher(line);
            if (m.matches()) {
                int num = Integer.parseInt(m.group(1));
                long instances = Long.parseLong(m.group(2));
                long bytes = Long.parseLong(m.group(3));
                String clazz = m.group(4).trim();

                // Parse class name and extract module information
                ClassInfo classInfo = ClassInfo.from(clazz);
                result.add(new HistogramEntry(num, instances, bytes, classInfo.className, classInfo.module, classInfo.isArray));
            }
        }
        return result;
    }

    static class ClassInfo {
        String className;
        String module;
        boolean isArray;

        ClassInfo(String className, String module, boolean isArray) {
            this.className = className;
            this.module = module;
            this.isArray = isArray;
        }

        /**
         * @param clazz className (module@version)" or just "className"
         */
        public static ClassInfo from(String clazz) {
            String className = clazz;
            String module = null;
            if (clazz.contains(" (")) {
                int parenIndex = clazz.lastIndexOf(" (");
                className = clazz.substring(0, parenIndex).trim();
                String modulePart = clazz.substring(parenIndex + 2, clazz.length() - 1).trim();

                // Extract module name (before @ if present)
                if (modulePart.contains("@")) {
                    module = modulePart.substring(0, modulePart.indexOf("@"));
                } else {
                    module = modulePart;
                }
            }

            // Check if it's an array type and normalize
            boolean isArray = false;
            if (className.startsWith("[")) {
                isArray = true;
                className = normalizeArrayType(className);
            }

            return new ClassInfo(className, module, isArray);
        }

        private static String normalizeArrayType(String arrayType) {
            if (arrayType.length() < 2) {
                return arrayType;
            }

            char type = arrayType.charAt(1);
            if (type == 'B') {
                return "byte[]";
            } else if (type == 'C') {
                return "char[]";
            } else if (type == 'D') {
                return "double[]";
            } else if (type == 'F') {
                return "float[]";
            } else if (type == 'I') {
                return "int[]";
            } else if (type == 'J') {
                return "long[]";
            } else if (type == 'S') {
                return "short[]";
            } else if (type == 'Z') {
                return "boolean[]";
            } else if (type == 'L') {
                // Object array: [Lpackage.ClassName;
                String objectType = arrayType.substring(2, arrayType.length() - 1);
                return objectType + "[]";
            } else if (type == '[') {
                // Multi-dimensional array
                int dimensions = 0;
                String baseType = arrayType;
                while (baseType.startsWith("[")) {
                    dimensions++;
                    baseType = baseType.substring(1);
                }

                String normalizedBase = normalizeArrayType("[" + baseType);
                return normalizedBase + "[]".repeat(Math.max(0, dimensions - 1));
            }

            // If we can't normalize, return as-is
            return arrayType;
        }
    }
}
