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

import lombok.Data;
import lombok.Getter;
import org.bithon.server.datasource.query.ast.IASTNode;
import org.bithon.server.datasource.reader.jdbc.dialect.ISqlDialect;
import org.bithon.server.datasource.reader.jdbc.statement.serializer.SelectStatementSerializer;

/**
 * Take SQL as an example, this AST node represents a whole SELECT statement.
 * Since statement is a concept in SQL, here we don't use that concept but use 'expression',
 * so this class is called as 'SelectExpression'
 *
 * @author frank.chen021@outlook.com
 * @date 2022/9/4 14:55
 */
@Data
public class SelectStatement implements IASTNode {
    @Getter
    private final SelectorList selectorList = new SelectorList();

    private final FromClause from = new FromClause();
    private final WhereClause where = new WhereClause();
    private final GroupByClause groupBy = new GroupByClause();
    private OrderByClause[] orderBy;
    private LimitClause limit;
    private HavingClause having;

    public void setOrderBy(OrderByClause... orderBy) {
        this.orderBy = orderBy;
    }

    public String toSQL(ISqlDialect sqlDialect) {
        SelectStatementSerializer generator = new SelectStatementSerializer(sqlDialect);
        generator.generate(this);
        return generator.getSQL();
    }
}
