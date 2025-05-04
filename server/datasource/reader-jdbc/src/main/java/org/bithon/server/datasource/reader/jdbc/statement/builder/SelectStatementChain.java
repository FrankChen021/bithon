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

package org.bithon.server.datasource.reader.jdbc.statement.builder;


import org.bithon.server.datasource.reader.jdbc.dialect.ISqlDialect;
import org.bithon.server.datasource.reader.jdbc.statement.ast.FromClause;
import org.bithon.server.datasource.reader.jdbc.statement.ast.SelectStatement;

import java.util.ArrayList;
import java.util.List;

/**
 * hain SELECT statements as nested queries.
 *
 * @author frank.chen021@outlook.com
 * @date 12/4/25 9:55 am
 */
class SelectStatementChain {
    final SelectStatement aggregation = new SelectStatement();
    SelectStatement windowAggregation;
    SelectStatement slidingWindowAggregation;
    SelectStatement postAggregation;
    SelectStatement outermost;
    SelectStatement innermost;

    int nestIndex = 0;

    public void chain(ISqlDialect sqlDialect) {
        // chain SELECT statement as a pipeline in reverse order,
        // which means the first SELECT statement is the innermost one
        List<SelectStatement> statements = new ArrayList<>();

        if (windowAggregation != null) {
            statements.add(windowAggregation);
        }

        statements.add(aggregation);

        if (slidingWindowAggregation != null) {
            statements.add(slidingWindowAggregation);
        }

        if (postAggregation != null) {
            statements.add(postAggregation);
        }

        for (int i = 1; i < statements.size(); i++) {
            FromClause from = statements.get(i).getFrom();
            from.setExpression(statements.get(i - 1));

            if (sqlDialect.needTableAlias()) {
                from.setAlias("tbl" + nestIndex++);
            }
        }

        this.outermost = statements.get(statements.size() - 1);
        this.innermost = statements.get(0);
    }
}
