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

import org.bithon.component.commons.expression.serialization.ExpressionSerializer;
import org.bithon.server.storage.datasource.query.ast.Alias;
import org.bithon.server.storage.datasource.query.ast.Column;
import org.bithon.server.storage.datasource.query.ast.Expression;
import org.bithon.server.storage.datasource.query.ast.From;
import org.bithon.server.storage.datasource.query.ast.GroupBy;
import org.bithon.server.storage.datasource.query.ast.IASTNodeVisitor;
import org.bithon.server.storage.datasource.query.ast.Limit;
import org.bithon.server.storage.datasource.query.ast.OrderBy;
import org.bithon.server.storage.datasource.query.ast.QueryExpression;
import org.bithon.server.storage.datasource.query.ast.Selector;
import org.bithon.server.storage.datasource.query.ast.Table;
import org.bithon.server.storage.datasource.query.ast.TextNode;
import org.bithon.server.storage.datasource.query.ast.Where;
import org.bithon.server.storage.jdbc.common.dialect.ISqlDialect;

import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2022/9/4 15:38
 */
public class SqlGenerator implements IASTNodeVisitor {

    private final StringBuilder sql = new StringBuilder(512);
    private final ISqlDialect sqlDialect;
    private int nestedSelect = 0;
    private String indent = "";

    private ExpressionSerializer expressionSerializer;

    public SqlGenerator(ISqlDialect sqlDialect) {
        this.sqlDialect = sqlDialect;
    }

    public String getSQL() {
        return sql.toString();
    }

    @Override
    public void before(QueryExpression queryExpression) {
        if (nestedSelect++ > 0) {
            sql.append('\n');
            sql.append(indent);
            sql.append('(');
            sql.append('\n');
            indent += "  ";
        }
        sql.append(indent);
        sql.append("SELECT ");
    }

    @Override
    public void visit(QueryExpression select) {
        select.accept(this);
    }

    @Override
    public void after(QueryExpression queryExpression) {
        if (--nestedSelect > 0) {
            indent = indent.substring(0, indent.length() - 2);

            sql.append('\n');
            sql.append(indent);
            sql.append(')');
        }
    }

    @Override
    public void visit(OrderBy orderBy) {
        sql.append('\n');
        sql.append(indent);
        sql.append("ORDER BY ");
        sql.append(sqlDialect.quoteIdentifier(orderBy.getField()));

        if (orderBy.getOrder() != null) {
            sql.append(' ');
            sql.append(orderBy.getOrder());
        }
    }

    @Override
    public void visit(Limit limit) {
        sql.append('\n');
        sql.append(indent);
        sql.append("LIMIT ");
        sql.append(limit.getLimit());
        if (limit.getOffset() > 0) {
            sql.append(" OFFSET ");
            sql.append(limit.getOffset());
        }
    }

    @Override
    public void visit(Expression expression) {
        sql.append(expressionSerializer.serialize(expression.getParsedExpression()));
    }

    @Override
    public void visit(TextNode textNode) {
        sql.append(textNode.getStr());
    }

    @Override
    public void visit(int index, int count, Selector selector) {
        selector.accept(this);
        if (index < count - 1) {
            sql.append(',');
            sql.append('\n');
            sql.append(indent);
        }
    }

    @Override
    public void visit(Column column) {
        sql.append(sqlDialect.quoteIdentifier(column.getName()));
    }

    @Override
    public void visit(Alias alias) {
        sql.append(" AS ");
        sql.append(sqlDialect.quoteIdentifier(alias.getName()));
    }

    @Override
    public void visit(From from) {
        sql.append('\n');
        sql.append(indent);
        sql.append("FROM");
    }

    @Override
    public void visit(Table table) {
        sql.append(' ');
        sql.append(sqlDialect.quoteIdentifier(table.getName()));
    }

    @Override
    public void visit(Where where) {
        sql.append('\n');
        sql.append(indent);
        sql.append("WHERE ");

        List<String> expressions = where.getExpressions();
        for (int i = 0, expressionsSize = expressions.size(); i < expressionsSize; i++) {
            if (i != 0) {
                sql.append(" AND ");
            }
            sql.append(expressions.get(i));
        }
    }

    @Override
    public void visit(GroupBy groupBy) {
        if (groupBy.getFields().isEmpty()) {
            return;
        }

        sql.append('\n');
        sql.append(indent);
        sql.append("GROUP BY ");

        List<String> fields = groupBy.getFields();
        for (int i = 0, fieldsSize = fields.size(); i < fieldsSize; i++) {
            if (i > 0) {
                sql.append(", ");
            }
            sql.append(sqlDialect.quoteIdentifier(fields.get(i)));
        }
    }
}
