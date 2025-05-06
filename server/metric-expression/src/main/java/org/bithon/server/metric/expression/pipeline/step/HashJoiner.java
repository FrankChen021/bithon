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

package org.bithon.server.metric.expression.pipeline.step;

import org.bithon.component.commons.utils.CollectionUtils;
import org.bithon.server.datasource.query.pipeline.Column;
import org.bithon.server.datasource.query.pipeline.ColumnarTable;
import org.bithon.server.datasource.query.pipeline.CompositeKey;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 7/4/25 10:20 am
 */
public class HashJoiner {

    public static List<Column> join(
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
        List<Column> resultColumns = new ArrayList<>(joinKeys.size() + leftValueColumns.size() + rightValueColumn.size());
        for (String col : joinKeys) {
            Column c = Column.create(col,
                                     left.getColumn(col).getDataType().name(),
                                     hashTable.size());
            resultColumns.add(c);
        }
        for (Column col : leftValueColumns) {
            Column c = Column.create(col.getName(),
                                     left.getColumn(col.getName()).getDataType(),
                                     hashTable.size());
            resultColumns.add(c);
        }
        for (Column col : rightValueColumn) {
            Column c = Column.create(col.getName(),
                                     right.getColumn(col.getName()).getDataType().name(),
                                     hashTable.size());
            resultColumns.add(c);
        }

        // Probe and build result
        List<Column> rightJoinColumns = joinKeys.stream()
                                                .map(right::getColumn)
                                                .toList();

        for (int rightRowIndex = 0; rightRowIndex < right.rowCount(); rightRowIndex++) {
            CompositeKey key = CompositeKey.from(rightJoinColumns, rightRowIndex);
            List<Integer> matchedLeftRows = hashTable.get(key);
            if (CollectionUtils.isEmpty(matchedLeftRows)) {
                continue;
            }

            for (int matchedLeftRow : matchedLeftRows) {
                int resultColumnIndex = 0;
                for (Column col : leftJoinColumns) {
                    resultColumns.get(resultColumnIndex++).addObject(col.getObject(matchedLeftRow));
                }
                for (Column col : leftValueColumns) {
                    resultColumns.get(resultColumnIndex++).addObject(col.getObject(matchedLeftRow));
                }
                for (Column col : rightValueColumn) {
                    resultColumns.get(resultColumnIndex++).addObject(col.getObject(rightRowIndex));
                }
            }
        }

        return resultColumns;
    }

    private static Map<CompositeKey, List<Integer>> toHashTable(ColumnarTable table,
                                                                List<Column> columns) {
        Map<CompositeKey, List<Integer>> hashTable = new HashMap<>();
        for (int i = 0, rows = table.rowCount(); i < rows; i++) {
            CompositeKey key = CompositeKey.from(columns, i);
            hashTable.computeIfAbsent(key, k -> new ArrayList<>())
                     .add(i);
        }
        return hashTable;
    }
}

