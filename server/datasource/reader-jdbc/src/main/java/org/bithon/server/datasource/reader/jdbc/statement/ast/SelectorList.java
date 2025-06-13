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

package org.bithon.server.datasource.reader.jdbc.statement.ast;

import lombok.Getter;
import org.bithon.component.commons.expression.IDataType;
import org.bithon.server.datasource.query.ast.Alias;
import org.bithon.server.datasource.query.ast.IASTNode;
import org.bithon.server.datasource.query.ast.Selector;

import java.util.ArrayList;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2022/9/4 14:56
 */
public class SelectorList implements IASTNode {
    @Getter
    private final List<Selector> selectors;

    public static SelectorList from(List<Selector> columns) {
        return new SelectorList(columns);
    }

    public SelectorList() {
        this.selectors = new ArrayList<>(4);
    }

    public SelectorList(List<Selector> selectors) {
        this.selectors = selectors;
    }

    /**
     * insert the column at first place
     */
    public SelectorList insert(String columnName, IDataType dataType) {
        this.selectors.add(0, new Selector(columnName, dataType));
        return this;
    }

    /**
     * insert the column at first place
     */
    public SelectorList insert(IASTNode columnExpression, IDataType dataType) {
        return insert(columnExpression, null, dataType);
    }

    public SelectorList insert(IASTNode columnExpression, String output, IDataType dataType) {
        if (columnExpression instanceof Selector) {
            throw new RuntimeException("Can't add typeof ResultColumn");
        }
        selectors.add(0, new Selector(columnExpression, output, dataType));
        return this;
    }

    public Selector add(IASTNode columnExpression, IDataType dataType) {
        return add(columnExpression, (Alias) null, dataType);
    }

    public Selector add(IASTNode columnExpression, String output, IDataType dataType) {
        return add(columnExpression, new Alias(output), dataType);
    }

    public Selector add(IASTNode columnExpression, Alias output, IDataType dataType) {
        if (columnExpression instanceof Selector) {
            throw new RuntimeException("Can't add typeof ResultColumn");
        }

        Selector selector = new Selector(columnExpression, output, dataType);
        selectors.add(selector);
        return selector;
    }

    public void addAll(List<Selector> columns) {
        selectors.addAll(columns);
    }

    public int size() {
        return selectors.size();
    }

    public Selector get(int index) {
        return selectors.get(index);
    }
}
