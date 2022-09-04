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

package org.bithon.server.storage.jdbc.metric;

import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.storage.jdbc.dsl.sql.AliasExpression;
import org.bithon.server.storage.jdbc.dsl.sql.FieldsExpression;
import org.bithon.server.storage.jdbc.dsl.sql.FromExpression;
import org.bithon.server.storage.jdbc.dsl.sql.FunctionExpression;
import org.bithon.server.storage.jdbc.dsl.sql.GroupByExpression;
import org.bithon.server.storage.jdbc.dsl.sql.IExpression;
import org.bithon.server.storage.jdbc.dsl.sql.IExpressionVisitor;
import org.bithon.server.storage.jdbc.dsl.sql.NameExpression;
import org.bithon.server.storage.jdbc.dsl.sql.OrderByExpression;
import org.bithon.server.storage.jdbc.dsl.sql.SelectExpression;
import org.bithon.server.storage.jdbc.dsl.sql.StringExpression;
import org.bithon.server.storage.jdbc.dsl.sql.TableExpression;
import org.bithon.server.storage.jdbc.dsl.sql.WhereExpression;

/**
 * @author frank.chen021@outlook.com
 * @date 2022/9/4 15:38
 */
public class SQLGenerator implements IExpressionVisitor {

    private final StringBuilder sb = new StringBuilder(512);
    private int nestedSelect = 0;

    public String getSQL() {
        return sb.toString();
    }

    @Override
    public void before(SelectExpression selectExpression) {
        if (nestedSelect++ > 0) {
            sb.append("( ");
        }
        sb.append("SELECT ");
    }

    @Override
    public void visit(SelectExpression select) {
        select.accept(this);
    }

    @Override
    public void after(SelectExpression selectExpression) {
        if (--nestedSelect > 0) {
            sb.append(") ");
        }
    }

    @Override
    public void visit(OrderByExpression orderByExpression) {
        sb.append("ORDER BY ");
        sb.append('\"');
        sb.append(orderByExpression.getField());
        sb.append('\"');

        if (!StringUtils.isEmpty(orderByExpression.getOrder())) {
            sb.append(' ');
            sb.append(orderByExpression.getOrder());
            sb.append(' ');
        }
    }

    @Override
    public void visit(FieldsExpression fieldsExpression) {
        for(IExpression field : fieldsExpression.getFields()) {
            field.accept(this);

            sb.append(',');
        }
        int last = sb.length() - 1;
        if (sb.charAt(last) == ',') {
            sb.delete(sb.length() - 1, sb.length());
        }
    }

    @Override
    public void before(FunctionExpression functionExpression) {

    }

    @Override
    public void after(FunctionExpression functionExpression) {

    }

    @Override
    public void visit(StringExpression stringExpression) {
        sb.append(stringExpression.getStr());
    }

    @Override
    public void visit(AliasExpression aliasExpression) {
        sb.append("AS");
        sb.append('\"');
        sb.append(aliasExpression.getName());
        sb.append('\"');
    }

    @Override
    public void visit(NameExpression nameExpression) {
        sb.append('\"');
        sb.append(nameExpression.getName());
        sb.append('\"');
        sb.append(' ');
    }

    @Override
    public void visit(FromExpression fromExpression) {
        sb.append(" FROM ");
    }

    @Override
    public void visit(TableExpression table) {
        sb.append('\"');
        sb.append(table.getName());
        sb.append('\"');
        sb.append(' ');
    }

    @Override
    public void visit(WhereExpression whereExpression) {
        sb.append("WHERE ");
        for (String expression : whereExpression.getExpressions()) {
            sb.append(expression);
            sb.append(" AND ");
        }
        sb.delete(sb.length() - 4, sb.length());
    }

    @Override
    public void visit(GroupByExpression groupByExpression) {
        sb.append("GROUP BY ");
        for (String field : groupByExpression.getFields()) {
            sb.append('\"');
            sb.append(field);
            sb.append('\"');
            sb.append(" ,");
        }
        sb.delete(sb.length() - 1, sb.length());
    }
}
