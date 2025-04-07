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


import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author frank.chen021@outlook.com
 * @date 7/4/25 10:16 am
 */
public class ColumnarTable {
    private final Map<String, Column<?>> columns = new LinkedHashMap<>();

    public <T> void addColumn(String name, Column<T> column) {
        columns.put(name, column);
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
        if (columns.isEmpty()) return 0;
        return columns.values().iterator().next().size();
    }

    public Map<String, Column<?>> getColumns() {
        return columns;
    }
}
