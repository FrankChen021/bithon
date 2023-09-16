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

import org.bithon.server.storage.datasource.query.OrderBy;
import org.bithon.server.storage.datasource.query.ast.ASTColumn;
import org.bithon.server.storage.datasource.query.ast.ASTColumnAlias;
import org.bithon.server.storage.datasource.query.ast.ASTExpression;
import org.bithon.server.storage.datasource.query.ast.ASTFrom;
import org.bithon.server.storage.datasource.query.ast.ASTFunction;
import org.bithon.server.storage.datasource.query.ast.ASTGroupBy;
import org.bithon.server.storage.datasource.query.ast.ASTLimit;
import org.bithon.server.storage.datasource.query.ast.ASTOrderBy;
import org.bithon.server.storage.datasource.query.ast.ASTResultColumn;
import org.bithon.server.storage.datasource.query.ast.ASTSelectExpression;
import org.bithon.server.storage.datasource.query.ast.ASTStringLiteral;
import org.bithon.server.storage.datasource.query.ast.ASTTable;
import org.bithon.server.storage.datasource.query.ast.ASTWhere;
import org.bithon.server.storage.datasource.query.ast.IASTNodeVisitor;

/**
 * @author frank.chen021@outlook.com
 * @date 2022/9/4 15:38
 */
public class SqlGenerator implements IASTNodeVisitor {

    private final StringBuilder sql = new StringBuilder(512);
    private int nestedSelect = 0;

    public String getSQL() {
        return sql.toString();
    }

    @Override
    public void before(ASTSelectExpression selectExpression) {
        if (nestedSelect++ > 0) {
            sql.append("( ");
        }
        sql.append("SELECT ");
    }

    @Override
    public void visit(ASTSelectExpression select) {
        select.accept(this);
    }

    @Override
    public void after(ASTSelectExpression selectExpression) {
        if (--nestedSelect > 0) {
            sql.append(") ");
        }
    }

    @Override
    public void visit(ASTOrderBy orderBy) {
        sql.append("ORDER BY ");

        for (int i = 0; i < orderBy.getOrders().length; i++) {
            if (i > 0) {
                sql.append(',');
            }

            OrderBy order = orderBy.getOrders()[i];
            sql.append('\"');
            sql.append(order.getName());
            sql.append('\"');

            if (order.getOrder() != null) {
                sql.append(' ');
                sql.append(order.getOrder());
                sql.append(' ');
            }
        }
    }

    @Override
    public void visit(ASTLimit limit) {
        sql.append(" LIMIT ");
        sql.append(limit.getLimit());
        if (limit.getOffset() > 0) {
            sql.append(" OFFSET ");
            sql.append(limit.getOffset());
        }
    }

    @Override
    public void visit(ASTExpression expression) {

    }

    @Override
    public void before(ASTFunction function) {
        sql.append(function.getFnName());
        sql.append('(');
    }

    @Override
    public void after(ASTFunction function) {
        sql.append(')');
    }

    @Override
    public void visit(ASTStringLiteral stringNode) {
        sql.append(stringNode.getStr());
    }

    @Override
    public void visit(int index, int count, ASTResultColumn resultColumn) {

        resultColumn.accept(this);

        if (index < count - 1) {
            sql.append(',');
        }
    }

    @Override
    public void visit(ASTColumn column) {
        sql.append('\"');
        sql.append(column);
        sql.append('\"');
        sql.append(' ');
    }

    @Override
    public void visit(ASTColumnAlias alias) {
        sql.append(" AS ");
        sql.append('\"');
        sql.append(alias.getName());
        sql.append('\"');
    }

    @Override
    public void visit(ASTFrom from) {
        sql.append(" FROM ");
    }

    @Override
    public void visit(ASTTable table) {
        sql.append('\"');
        sql.append(table.getName());
        sql.append('\"');
        sql.append(' ');
    }

    @Override
    public void visit(ASTWhere where) {
        sql.append("WHERE ");
        for (String expression : where.getExpressions()) {
            sql.append(expression);
            sql.append(" AND ");
        }
        sql.delete(sql.length() - 4, sql.length());
    }

    @Override
    public void visit(ASTGroupBy groupBy) {
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
