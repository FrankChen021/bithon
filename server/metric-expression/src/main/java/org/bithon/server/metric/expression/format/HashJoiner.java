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

import org.bithon.component.commons.utils.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
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
        List<String> joinKeys,
        List<String> leftValueColumns,
        List<String> rightValueColumn) {
        return hashJoinInternal(left,
                                right,
                                joinKeys,
                                leftValueColumns.stream().map(left::getColumn).toList(),
                                rightValueColumn.stream().map(right::getColumn).toList());
    }

    public static ColumnarTable hashJoinInternal(
        ColumnarTable left,
        ColumnarTable right,
        List<String> joinKeys,
        List<Column> leftValueColumns,
        List<Column> rightValueColumn) {
        List<Column> leftJoinColumns = joinKeys.stream()
                                               .map(left::getColumn)
                                               .toList();

        //
        // Build hash table for key probe
        //
        Map<CompositeKey, List<Integer>> hashTable = toHashTable(left, leftJoinColumns);

        //
        // Prepare result
        //
        ColumnarTable resultTable = new ColumnarTable();
        List<Column> resultColumns = new ArrayList<>(joinKeys.size() + leftValueColumns.size() + rightValueColumn.size());
        for (String col : joinKeys) {
            Column c = resultTable.addColumn(Column.create(col,
                                                           left.getColumn(col).getDataType().name(),
                                                           hashTable.size()));
            resultColumns.add(c);
        }
        for (Column col : leftValueColumns) {
            Column c = resultTable.addColumn(Column.create(col.getName(),
                                                           left.getColumn(col.getName()).getDataType(),
                                                           hashTable.size()));
            resultColumns.add(c);
        }
        for (Column col : rightValueColumn) {
            Column c = resultTable.addColumn(Column.create(col.getName(),
                                                           right.getColumn(col.getName()).getDataType().name(),
                                                           hashTable.size()));
            resultColumns.add(c);
        }

        // Probe and build result
        List<Column> rightJoinColumns = joinKeys.stream()
                                                .map(right::getColumn)
                                                .toList();

        for (int i = 0; i < right.rowCount(); i++) {
            CompositeKey key = extractKey(right, rightJoinColumns, i);
            List<Integer> matchedRows = hashTable.get(key);
            if (CollectionUtils.isEmpty(matchedRows)) {
                continue;
            }

            for (int row : matchedRows) {
                int resultColumnIndex = 0;
                for (Column col : leftJoinColumns) {
                    resultColumns.get(resultColumnIndex++).addObject(col.getObject(row));
                }
                for (Column col : leftValueColumns) {
                    resultColumns.get(resultColumnIndex++).addObject(col.getObject(row));
                }
                for (Column col : rightValueColumn) {
                    resultColumns.get(resultColumnIndex++).addObject(col.getObject(row));
                }
            }
        }

        return resultTable;
    }

    private static CompositeKey extractKey(ColumnarTable table,
                                           List<Column> columns,
                                           int row) {
        Object[] parts = new Object[columns.size()];
        for (int i = 0, size = columns.size(); i < size; i++) {
            parts[i] = columns.get(i).getObject(row);
        }
        return new CompositeKey(parts);
    }

    private static Map<CompositeKey, List<Integer>> toHashTable(ColumnarTable table,
                                                                List<Column> columns) {
        Map<CompositeKey, List<Integer>> hashTable = new HashMap<>();
        for (int i = 0, rows = table.rowCount(); i < rows; i++) {
            CompositeKey key = extractKey(table, columns, i);
            hashTable.computeIfAbsent(key, k -> new ArrayList<>())
                     .add(i);
        }
        return hashTable;
    }
}

