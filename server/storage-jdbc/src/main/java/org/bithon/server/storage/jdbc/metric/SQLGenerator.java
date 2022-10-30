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
import org.bithon.server.storage.jdbc.dsl.sql.LimitExpression;
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

    private final StringBuilder sql = new StringBuilder(512);
    private int nestedSelect = 0;

    public String getSQL() {
        return sql.toString();
    }

    @Override
    public void before(SelectExpression selectExpression) {
        if (nestedSelect++ > 0) {
            sql.append("( ");
        }
        sql.append("SELECT ");
    }

    @Override
    public void visit(SelectExpression select) {
        select.accept(this);
    }

    @Override
    public void after(SelectExpression selectExpression) {
        if (--nestedSelect > 0) {
            sql.append(") ");
        }
    }

    @Override
    public void visit(OrderByExpression orderByExpression) {
        sql.append("ORDER BY ");
        sql.append('\"');
        sql.append(orderByExpression.getField());
        sql.append('\"');

        if (!StringUtils.isEmpty(orderByExpression.getOrder())) {
            sql.append(' ');
            sql.append(orderByExpression.getOrder());
            sql.append(' ');
        }
    }

    @Override
    public void visit(FieldsExpression fieldsExpression) {
        for (IExpression field : fieldsExpression.getFields()) {
            field.accept(this);

            sql.append(',');
        }
        int last = sql.length() - 1;
        if (sql.charAt(last) == ',') {
            sql.delete(sql.length() - 1, sql.length());
        }
    }

    @Override
    public void visit(LimitExpression limitExpression) {
        sql.append(" LIMIT ");
        sql.append(limitExpression.getLimit());
        if (limitExpression.getOffset() != null && limitExpression.getOffset() > 0) {
            sql.append(" OFFSET ");
            sql.append(limitExpression.getOffset());
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
        sql.append(stringExpression.getStr());
    }

    @Override
    public void visit(AliasExpression aliasExpression) {
        sql.append("AS");
        sql.append('\"');
        sql.append(aliasExpression.getName());
        sql.append('\"');
    }

    @Override
    public void visit(NameExpression nameExpression) {
        sql.append('\"');
        sql.append(nameExpression.getName());
        sql.append('\"');
        sql.append(' ');
    }

    @Override
    public void visit(FromExpression fromExpression) {
        sql.append(" FROM ");
    }

    @Override
    public void visit(TableExpression table) {
        sql.append('\"');
        sql.append(table.getName());
        sql.append('\"');
        sql.append(' ');
    }

    @Override
    public void visit(WhereExpression whereExpression) {
        sql.append("WHERE ");
        for (String expression : whereExpression.getExpressions()) {
            sql.append(expression);
            sql.append(" AND ");
        }
        sql.delete(sql.length() - 4, sql.length());
    }

    @Override
    public void visit(GroupByExpression groupByExpression) {
        sql.append("GROUP BY ");
        for (String field : groupByExpression.getFields()) {
            sql.append('\"');
            sql.append(field);
            sql.append('\"');
            sql.append(" ,");
        }
        sql.delete(sql.length() - 1, sql.length());
    }
}
