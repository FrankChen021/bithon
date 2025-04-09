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

package org.bithon.server.metric.expression.format;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 7/4/25 10:20 am
 */
public class HashJoiner {

    public static ColumnarTable hashJoin(
        ColumnarTable left,
        ColumnarTable right,
        List<String> joinKeys
    ) {
        // Build hash table
        Map<CompositeKey, List<Integer>> hashTable = new HashMap<>();
        for (int i = 0; i < left.rowCount(); i++) {
            CompositeKey key = extractKey(left, joinKeys, i);
            hashTable.computeIfAbsent(key, k -> new ArrayList<>()).add(i);
        }

        // Prepare result columns
        Map<String, List<Object>> resultData = new LinkedHashMap<>();
        for (String col : left.getColumnNames()) {
            resultData.put("left." + col, new ArrayList<>());
        }
        for (String col : right.getColumnNames()) {
            if (!joinKeys.contains(col)) {
                resultData.put("right." + col, new ArrayList<>());
            }
        }

        // Probe and build result
        for (int i = 0; i < right.rowCount(); i++) {
            CompositeKey key = extractKey(right, joinKeys, i);
            List<Integer> matches = hashTable.get(key);
            if (matches == null) {
                continue;
            }

            for (int li : matches) {
                for (String col : left.getColumnNames()) {
                    resultData.get("left." + col).add(left.getColumn(col).getObject(li));
                }
                for (String col : right.getColumnNames()) {
                    if (!joinKeys.contains(col)) {
                        resultData.get("right." + col).add(right.getColumn(col).getObject(i));
                    }
                }
            }
        }

        // Assemble result table
        ColumnarTable result = new ColumnarTable();
        for (Map.Entry<String, List<Object>> entry : resultData.entrySet()) {
            String name = entry.getKey();
            List<Object> data = entry.getValue();
            if (data.isEmpty()) {
                continue;
            }
            Object first = data.get(0);
            if (first instanceof Long) {
                Column.LongColumn col = new Column.LongColumn(data.size());
                for (Object o : data) {
                    col.addLong((long) o);
                }
                result.addColumn(name, col);
            } else if (first instanceof String) {
                Column.StringColumn col = new Column.StringColumn(data.size());
                for (Object o : data) {
                    col.addString((String) o);
                }
                result.addColumn(name, col);
            } else if (first instanceof Double) {
                Column.DoubleColumn col = new Column.DoubleColumn(data.size());
                for (Object o : data) {
                    col.addDouble((double) o);
                }
                result.addColumn(name, col);
            }
            // Add more type cases as needed
        }

        return result;
    }

    private static CompositeKey extractKey(ColumnarTable table, List<String> keys, int row) {
        Object[] parts = new Object[keys.size()];
        for (int i = 0; i < keys.size(); i++) {
            parts[i] = table.getColumn(keys.get(i)).getObject(row);
        }
        return new CompositeKey(parts);
    }
}

