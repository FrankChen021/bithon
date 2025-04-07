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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author frank.chen021@outlook.com
 * @date 7/4/25 10:16 am
 */
public class ColumnarTable {

    public static ColumnarTable of(String name, Column<?> column) {
        ColumnarTable table = new ColumnarTable();
        table.addColumn(name, column);
        return table;
    }

    private final Map<String, Column<?>> columns = new LinkedHashMap<>();

    public <T> Column<T> addColumn(String name, Column<T> column) {
        columns.put(name, column);
        return column;
    }

    public Column<?> getColumn(String name) {
        return columns.get(name);
    }

    public <T> Column<T> getColumnTyped(String name, Class<T> clazz) {
        //noinspection unchecked
        return (Column<T>) columns.get(name);
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
        List<Column> result = new ArrayList<>(names.size());
        for (String name : names) {
            Column<?> column = columns.get(name);
            if (column == null) {
                throw new IllegalArgumentException("Column " + name + " not found");
            }
            result.add(column);
        }
        return result;
    }

    public final static ColumnarTable EMPTY = new ColumnarTable();
}
