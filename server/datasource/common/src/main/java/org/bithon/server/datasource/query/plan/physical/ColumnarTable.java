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

package org.bithon.server.datasource.query.plan.physical;


import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author frank.chen021@outlook.com
 * @date 7/4/25 10:16 am
 */
public class ColumnarTable {

    public static ColumnarTable of(Column... columns) {
        ColumnarTable table = new ColumnarTable();
        for (Column column : columns) {
            table.addColumn(column);
        }
        return table;
    }

    /**
     * Column name -> Column
     */
    private final Map<String, Column> columns = new LinkedHashMap<>();

    public Column addColumn(Column column) {
        columns.put(column.getName(), column);
        return column;
    }

    public void addColumns(Collection<Column> columns) {
        for (Column column : columns) {
            this.columns.put(column.getName(), column);
        }
    }

    public Column getColumn(String name) {
        return columns.get(name);
    }

    public Set<String> getColumnNames() {
        return columns.keySet();
    }

    public int rowCount() {
        if (columns.isEmpty()) {
            return 0;
        }
        return columns.values().iterator().next().size();
    }

    public List<Column> getColumns(List<String> names) {
        if (names.isEmpty()) {
            return List.of();
        }

        List<Column> result = new ArrayList<>(names.size());
        for (String name : names) {
            Column column = columns.get(name);
            if (column == null) {
                throw new IllegalArgumentException("Column " + name + " not found");
            }
            result.add(column);
        }
        return result;
    }

    public List<Column> getColumns() {
        return columns.values().stream().toList();
    }

    /**
     * Add a new row to the table. Which helps simplify tests
     */
    public void addRow(Object... values) {
        if (values.length != columns.size()) {
            throw new IllegalArgumentException("Number of values does not match number of columns");
        }

        int i = 0;
        for (Column column : columns.values()) {
            column.addObject(values[i++]);
        }
    }

    public List<Map<String, Object>> toRowFormat() {
        int rowCount = rowCount();
        List<Map<String, Object>> rows = new ArrayList<>(rowCount);
        for (int i = 0; i < rowCount; i++) {
            rows.add(new LinkedHashMap<>());
        }
        for (Column column : columns.values()) {
            String name = column.getName();
            for (int i = 0; i < rowCount; i++) {
                rows.get(i).put(name, column.getObject(i));
            }
        }
        return rows;
    }

    public ColumnarTable filter(BitSet mask) {
        if (mask.cardinality() == rowCount()) {
            // All rows are include
            return this;
        }

        ColumnarTable newTable = new ColumnarTable();
        for (Map.Entry<String, Column> entry : this.columns.entrySet()) {
            newTable.addColumn(entry.getValue().filter(mask));
        }
        return newTable;
    }

    public ColumnarTable view(int[] selections, int length) {
        if (length == rowCount()) {
            // All rows are include
            return this;
        }

        ColumnarTable newTable = new ColumnarTable();
        for (Map.Entry<String, Column> entry : this.columns.entrySet()) {
            newTable.addColumn(entry.getValue().view(selections, length));
        }
        return newTable;
    }
}
