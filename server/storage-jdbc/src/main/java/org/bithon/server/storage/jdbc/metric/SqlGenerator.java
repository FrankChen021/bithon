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

import org.bithon.server.storage.datasource.query.ast.Column;
import org.bithon.server.storage.datasource.query.ast.ColumnAlias;
import org.bithon.server.storage.datasource.query.ast.Expression;
import org.bithon.server.storage.datasource.query.ast.From;
import org.bithon.server.storage.datasource.query.ast.Function;
import org.bithon.server.storage.datasource.query.ast.GroupBy;
import org.bithon.server.storage.datasource.query.ast.IASTNodeVisitor;
import org.bithon.server.storage.datasource.query.ast.Limit;
import org.bithon.server.storage.datasource.query.ast.OrderBy;
import org.bithon.server.storage.datasource.query.ast.ResultColumn;
import org.bithon.server.storage.datasource.query.ast.SelectExpression;
import org.bithon.server.storage.datasource.query.ast.StringNode;
import org.bithon.server.storage.datasource.query.ast.Table;
import org.bithon.server.storage.datasource.query.ast.Where;
import org.bithon.server.storage.jdbc.common.dialect.ISqlDialect;

/**
 * @author frank.chen021@outlook.com
 * @date 2022/9/4 15:38
 */
public class SqlGenerator implements IASTNodeVisitor {

    private final StringBuilder sql = new StringBuilder(512);
    private final ISqlDialect sqlDialect;
    private int nestedSelect = 0;

    public SqlGenerator(ISqlDialect sqlDialect) {
        this.sqlDialect = sqlDialect;
    }

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
    public void visit(OrderBy orderBy) {
        sql.append("ORDER BY ");
        sql.append(sqlDialect.quoteIdentifier(orderBy.getField()));

        if (orderBy.getOrder() != null) {
            sql.append(' ');
            sql.append(orderBy.getOrder());
            sql.append(' ');
        }
    }

    @Override
    public void visit(Limit limit) {
        sql.append(" LIMIT ");
        sql.append(limit.getLimit());
        if (limit.getOffset() > 0) {
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
    public void visit(StringNode stringNode) {
        sql.append(stringNode.getStr());
    }

    @Override
    public void visit(int index, int count, ResultColumn resultColumn) {

        resultColumn.accept(this);

        if (index < count - 1) {
            sql.append(',');
        }
    }

    @Override
    public void visit(Column column) {
        sql.append(sqlDialect.quoteIdentifier(column.getName()));
    }

    @Override
    public void visit(ColumnAlias alias) {
        sql.append(" AS ");
        sql.append(sqlDialect.quoteIdentifier(alias.getName()));
    }

    @Override
    public void visit(From from) {
        sql.append(" FROM ");
    }

    @Override
    public void visit(Table table) {
        sql.append(sqlDialect.quoteIdentifier(table.getName()));
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
            sql.append(sqlDialect.quoteIdentifier(field));
            sql.append(" ,");
        }
        sql.delete(sql.length() - 1, sql.length());
    }
}
