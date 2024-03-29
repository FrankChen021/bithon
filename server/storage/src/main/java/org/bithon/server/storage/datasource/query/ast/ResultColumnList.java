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
public class ResultColumnList implements IASTNode {
    @Getter
    private final List<ResultColumn> columns;

    public static ResultColumnList from(List<ResultColumn> columns) {
        return new ResultColumnList(columns);
    }

    public ResultColumnList() {
        this.columns = new ArrayList<>(4);
    }

    public ResultColumnList(List<ResultColumn> columns) {
        this.columns = columns;
    }

    /**
     * insert the column at first place
     */
    public ResultColumnList insert(IASTNode columnExpression) {
        if (columnExpression instanceof ResultColumn) {
            throw new RuntimeException("Can't add typeof ResultColumn");
        }
        columns.add(0, new ResultColumn(columnExpression));
        return this;
    }

    public ResultColumnList add(String columnName) {
        this.columns.add(new ResultColumn(columnName));
        return this;
    }

    public ResultColumnList add(IASTNode columnExpression) {
        if (columnExpression instanceof ResultColumn) {
            throw new RuntimeException("Can't add typeof ResultColumn");
        }

        columns.add(new ResultColumn(columnExpression));
        return this;
    }

    public ResultColumnList add(IASTNode columnExpression, String columnAlias) {
        if (columnExpression instanceof ResultColumn) {
            throw new RuntimeException("Can't add typeof ResultColumn");
        }
        columns.add(new ResultColumn(columnExpression, columnAlias));
        return this;
    }

    public ResultColumnList add(IASTNode columnExpression, ColumnAlias columnAlias) {
        if (columnExpression instanceof ResultColumn) {
            throw new RuntimeException("Can't add typeof ResultColumn");
        }
        columns.add(new ResultColumn(columnExpression, columnAlias));
        return this;
    }

    public ResultColumnList addAll(List<String> columns) {
        for (String column : columns) {
            this.columns.add(new ResultColumn(column));
        }
        return this;
    }

    public <C> C getColumnNames(Collector<String, ?, C> collector) {
        return columns.stream()
                      .map(ResultColumn::getResultColumnName)
                      .collect(collector);
    }

    @Override
    public void accept(IASTNodeVisitor visitor) {
        for (int i = 0, size = this.columns.size(); i < size; i++) {
            visitor.visit(i, size, this.columns.get(i));
        }
    }
}
