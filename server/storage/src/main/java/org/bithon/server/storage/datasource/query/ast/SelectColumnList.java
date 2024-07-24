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

package org.bithon.server.storage.datasource.query.ast;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collector;

/**
 * @author frank.chen021@outlook.com
 * @date 2022/9/4 14:56
 */
public class SelectColumnList implements IASTNode {
    @Getter
    private final List<SelectColumn> columns;

    public static SelectColumnList from(List<SelectColumn> columns) {
        return new SelectColumnList(columns);
    }

    public SelectColumnList() {
        this.columns = new ArrayList<>(4);
    }

    public SelectColumnList(List<SelectColumn> columns) {
        this.columns = columns;
    }

    /**
     * insert the column at first place
     */
    public SelectColumnList insert(IASTNode columnExpression) {
        if (columnExpression instanceof SelectColumn) {
            throw new RuntimeException("Can't add typeof ResultColumn");
        }
        columns.add(0, new SelectColumn(columnExpression));
        return this;
    }

    public SelectColumnList add(String columnName) {
        this.columns.add(new SelectColumn(columnName));
        return this;
    }

    public SelectColumnList add(IASTNode columnExpression) {
        if (columnExpression instanceof SelectColumn) {
            throw new RuntimeException("Can't add typeof ResultColumn");
        }

        columns.add(new SelectColumn(columnExpression));
        return this;
    }

    public SelectColumnList add(IASTNode columnExpression, String columnAlias) {
        if (columnExpression instanceof SelectColumn) {
            throw new RuntimeException("Can't add typeof ResultColumn");
        }
        columns.add(new SelectColumn(columnExpression, columnAlias));
        return this;
    }

    public SelectColumnList add(IASTNode columnExpression, ColumnAlias columnAlias) {
        if (columnExpression instanceof SelectColumn) {
            throw new RuntimeException("Can't add typeof ResultColumn");
        }
        columns.add(new SelectColumn(columnExpression, columnAlias));
        return this;
    }

    public SelectColumnList addAll(List<String> columns) {
        for (String column : columns) {
            this.columns.add(new SelectColumn(column));
        }
        return this;
    }

    public <C> C getColumnNames(Collector<String, ?, C> collector) {
        return columns.stream()
                      .map(SelectColumn::getResultColumnName)
                      .collect(collector);
    }

    @Override
    public void accept(IASTNodeVisitor visitor) {
        for (int i = 0, size = this.columns.size(); i < size; i++) {
            visitor.visit(i, size, this.columns.get(i));
        }
    }
}
