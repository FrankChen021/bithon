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

package org.bithon.server.datasource.reader.jdbc.statement;

import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.utils.CollectionUtils;
import org.bithon.server.datasource.query.ast.Alias;
import org.bithon.server.datasource.query.ast.Column;
import org.bithon.server.datasource.query.ast.Expression;
import org.bithon.server.datasource.query.ast.Selector;
import org.bithon.server.datasource.reader.jdbc.dialect.Expression2Sql;
import org.bithon.server.datasource.reader.jdbc.dialect.ISqlDialect;
import org.bithon.server.datasource.reader.jdbc.statement.ast.FromClause;
import org.bithon.server.datasource.reader.jdbc.statement.ast.GroupByClause;
import org.bithon.server.datasource.reader.jdbc.statement.ast.HavingClause;
import org.bithon.server.datasource.reader.jdbc.statement.ast.LimitClause;
import org.bithon.server.datasource.reader.jdbc.statement.ast.OrderByClause;
import org.bithon.server.datasource.reader.jdbc.statement.ast.SelectStatement;
import org.bithon.server.datasource.reader.jdbc.statement.ast.TableIdentifier;
import org.bithon.server.datasource.reader.jdbc.statement.ast.TextNode;
import org.bithon.server.datasource.reader.jdbc.statement.ast.WhereClause;

import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2022/9/4 15:38
 */
public class SqlGenerator {

    private final StringBuilder sql = new StringBuilder(512);
    private final ISqlDialect sqlDialect;
    private int nestedSelect = 0;
    private String indent = "";

    public SqlGenerator(ISqlDialect sqlDialect) {
        this.sqlDialect = sqlDialect;
    }

    public String getSQL() {
        return sql.toString();
    }

    public void generate(SelectStatement selectStatement) {
        beforeSelectStatement();
        {
            for (int i = 0, size = selectStatement.getSelectorList().size(); i < size; i++) {
                Selector selector = selectStatement.getSelectorList().get(i);
                this.visit(i, size, selector);
            }

            this.visit(selectStatement.getFrom());

            if (!selectStatement.getWhere().isEmpty()) {
                this.visit(selectStatement.getWhere());
            }

            if (!selectStatement.getGroupBy().isEmpty()) {
                this.visit(selectStatement.getGroupBy());
            }

            if (selectStatement.getHaving() != null) {
                this.visit(selectStatement.getHaving());
            }

            if (selectStatement.getOrderBy() != null && selectStatement.getOrderBy().length > 0) {
                this.visit(selectStatement.getOrderBy());
            }

            if (selectStatement.getLimit() != null) {
                this.visit(selectStatement.getLimit());
            }
        }
        afterSelectStatement();
    }

    private void beforeSelectStatement() {
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

    private void afterSelectStatement() {
        if (--nestedSelect > 0) {
            indent = indent.substring(0, indent.length() - 2);

            sql.append('\n');
            sql.append(indent);
            sql.append(')');
        }
    }

    private void visit(OrderByClause... orderBys) {
        sql.append('\n');
        sql.append(indent);
        sql.append("ORDER BY ");

        for (int i = 0, orderBysLength = orderBys.length; i < orderBysLength; i++) {
            OrderByClause orderBy = orderBys[i];
            if (i > 0) {
                sql.append(", ");
            }
            sql.append(sqlDialect.quoteIdentifier(orderBy.getField()));
            if (orderBy.getOrder() != null) {
                sql.append(' ');
                sql.append(orderBy.getOrder());
            }
        }
    }

    private void visit(LimitClause limit) {
        sql.append('\n');
        sql.append(indent);
        sql.append("LIMIT ");
        sql.append(limit.getLimit());
        if (limit.getOffset() > 0) {
            sql.append(" OFFSET ");
            sql.append(limit.getOffset());
        }
    }

    private void visit(Expression expression) {
        IExpression parsedExpression = expression.getParsedExpression();

        String serialized = new Expression2SqlSerializer(this.sqlDialect).serialize(parsedExpression);
        this.sql.append(serialized);
    }

    private void visit(TextNode textNode) {
        sql.append(textNode.getStr());
    }

    private void visit(int index, int count, Selector selector) {
        if (index == 0) {
            indent += "       ";
        }
        if (selector.getSelectExpression() instanceof Column column) {
            this.visit(column);
        } else if (selector.getSelectExpression() instanceof Expression expressionColumn) {
            this.visit(expressionColumn);
        } else if (selector.getSelectExpression() instanceof TextNode textNode) {
            this.visit(textNode);
        } else {
            throw new RuntimeException("Unsupported expression type: " + selector.getSelectExpression().getClass());
        }
        if (selector.getOutput() != null) {
            this.visit(selector.getOutput());
        }

        if (index < count - 1) {
            sql.append(',');
            sql.append('\n');
            sql.append(indent);
        } else {
            indent = indent.substring(0, indent.length() - 7);
        }
    }

    private void visit(Column column) {
        sql.append(sqlDialect.quoteIdentifier(column.getName()));
    }

    private void visit(Alias alias) {
        sql.append(" AS ");
        sql.append(sqlDialect.quoteIdentifier(alias.getName()));
    }

    private void visit(FromClause from) {
        sql.append('\n');
        sql.append(indent);
        sql.append("FROM");

        if (from.getExpression() instanceof TableIdentifier tableIdentifier) {
            this.visit(tableIdentifier);
        } else if (from.getExpression() instanceof SelectStatement) {
            this.generate((SelectStatement) from.getExpression());
        } else {
            throw new RuntimeException("Unsupported expression type: " + from.getExpression().getClass());
        }

        if (from.getAlias() != null) {
            this.visit(from.getAlias());
        }
    }

    private void visit(TableIdentifier table) {
        sql.append(' ');
        if (table.getIdentifier().isQualified()) {
            sql.append(sqlDialect.quoteIdentifier(table.getIdentifier().getQualifier()));
            sql.append('.');
        }
        sql.append(sqlDialect.quoteIdentifier(table.getIdentifier().getIdentifier()));
    }

    private void visit(WhereClause where) {
        if (where.isEmpty()) {
            return;
        }

        sql.append('\n');
        sql.append(indent);
        sql.append("WHERE ");

        List<IExpression> expressions = where.getExpressions();
        for (int i = 0, expressionsSize = expressions.size(); i < expressionsSize; i++) {
            IExpression expression = expressions.get(i);
            if (i != 0) {
                sql.append(" AND ");
            }

            if (expressionsSize > 1) {
                sql.append('(');
            }
            sql.append(new Expression2Sql(null, this.sqlDialect).serialize(expression));
            if (expressionsSize > 1) {
                sql.append(')');
            }
        }
    }

    private void visit(GroupByClause groupBy) {
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

    private void visit(HavingClause having) {
        if (CollectionUtils.isEmpty(having.getExpressions())) {
            return;
        }

        sql.append('\n');
        sql.append(indent);
        sql.append("HAVING ");

        List<String> expressions = having.getExpressions();
        for (int i = 0, expressionsSize = expressions.size(); i < expressionsSize; i++) {
            if (i != 0) {
                sql.append(" AND ");
            }
            sql.append(expressions.get(i));
        }
    }
}
