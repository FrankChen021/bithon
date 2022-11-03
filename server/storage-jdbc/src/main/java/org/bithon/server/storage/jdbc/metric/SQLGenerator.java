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
import org.bithon.server.storage.datasource.query.ast.Alias;
import org.bithon.server.storage.datasource.query.ast.Expression;
import org.bithon.server.storage.datasource.query.ast.Fields;
import org.bithon.server.storage.datasource.query.ast.From;
import org.bithon.server.storage.datasource.query.ast.Function;
import org.bithon.server.storage.datasource.query.ast.GroupBy;
import org.bithon.server.storage.datasource.query.ast.IAST;
import org.bithon.server.storage.datasource.query.ast.IASTVisitor;
import org.bithon.server.storage.datasource.query.ast.Limit;
import org.bithon.server.storage.datasource.query.ast.Name;
import org.bithon.server.storage.datasource.query.ast.OrderBy;
import org.bithon.server.storage.datasource.query.ast.SelectStatement;
import org.bithon.server.storage.datasource.query.ast.StringExpression;
import org.bithon.server.storage.datasource.query.ast.Table;
import org.bithon.server.storage.datasource.query.ast.Where;

/**
 * @author frank.chen021@outlook.com
 * @date 2022/9/4 15:38
 */
public class SQLGenerator implements IASTVisitor {

    private final StringBuilder sql = new StringBuilder(512);
    private int nestedSelect = 0;

    public String getSQL() {
        return sql.toString();
    }

    @Override
    public void before(SelectStatement selectStatement) {
        if (nestedSelect++ > 0) {
            sql.append("( ");
        }
        sql.append("SELECT ");
    }

    @Override
    public void visit(SelectStatement select) {
        select.accept(this);
    }

    @Override
    public void after(SelectStatement selectStatement) {
        if (--nestedSelect > 0) {
            sql.append(") ");
        }
    }

    @Override
    public void visit(OrderBy orderBy) {
        sql.append("ORDER BY ");
        sql.append('\"');
        sql.append(orderBy.getField());
        sql.append('\"');

        if (!StringUtils.isEmpty(orderBy.getOrder())) {
            sql.append(' ');
            sql.append(orderBy.getOrder());
            sql.append(' ');
        }
    }

    @Override
    public void visit(Fields fields) {
        for (IAST field : fields.getFields()) {
            field.accept(this);

            sql.append(',');
        }
        int last = sql.length() - 1;
        if (sql.charAt(last) == ',') {
            sql.delete(sql.length() - 1, sql.length());
        }
    }

    @Override
    public void visit(Limit limit) {
        sql.append(" LIMIT ");
        sql.append(limit.getLimit());
        if (limit.getOffset() != null && limit.getOffset() > 0) {
            sql.append(" OFFSET ");
            sql.append(limit.getOffset());
        }
    }

    @Override
    public void visit(Expression expression) {

    }

    @Override
    public void before(Function function) {
        sql.append(function.getFnName());
        sql.append('(');
    }

    @Override
    public void after(Function function) {
        sql.append(')');
    }

    @Override
    public void visit(StringExpression stringExpression) {
        sql.append(stringExpression.getStr());
    }

    @Override
    public void visit(Alias alias) {
        sql.append(" AS ");
        sql.append('\"');
        sql.append(alias.getName());
        sql.append('\"');
    }

    @Override
    public void visit(Name name) {
        sql.append('\"');
        sql.append(name.getName());
        sql.append('\"');
        sql.append(' ');
    }

    @Override
    public void visit(From from) {
        sql.append(" FROM ");
    }

    @Override
    public void visit(Table table) {
        sql.append('\"');
        sql.append(table.getName());
        sql.append('\"');
        sql.append(' ');
    }

    @Override
    public void visit(Where where) {
        sql.append("WHERE ");
        for (String expression : where.getExpressions()) {
            sql.append(expression);
            sql.append(" AND ");
        }
        sql.delete(sql.length() - 4, sql.length());
    }

    @Override
    public void visit(GroupBy groupBy) {
        sql.append("GROUP BY ");
        for (String field : groupBy.getFields()) {
            sql.append('\"');
            sql.append(field);
            sql.append('\"');
            sql.append(" ,");
        }
        sql.delete(sql.length() - 1, sql.length());
    }
}
