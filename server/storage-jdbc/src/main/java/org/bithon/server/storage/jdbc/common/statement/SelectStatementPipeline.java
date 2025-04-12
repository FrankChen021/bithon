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

package org.bithon.server.storage.jdbc.common.statement;


import org.bithon.server.storage.datasource.query.ast.FromClause;
import org.bithon.server.storage.datasource.query.ast.SelectStatement;
import org.bithon.server.storage.jdbc.common.dialect.ISqlDialect;

import java.util.ArrayList;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 12/4/25 9:55 am
 */
class SelectStatementPipeline {
    final SelectStatement aggregation = new SelectStatement();
    SelectStatement windowAggregation;
    SelectStatement postAggregation;
    SelectStatement outermost;
    SelectStatement innermost;

    int nestIndex = 0;

    public void chain(ISqlDialect sqlDialect) {
        List<SelectStatement> pipelines = new ArrayList<>();
        if (windowAggregation != null) {
            pipelines.add(windowAggregation);
        }

        pipelines.add(aggregation);

        if (postAggregation != null) {
            pipelines.add(postAggregation);
        }

        for (int i = 1; i < pipelines.size(); i++) {
            FromClause from = pipelines.get(i).getFrom();
            from.setExpression(pipelines.get(i - 1));

            if (sqlDialect.needTableAlias()) {
                from.setAlias("tbl" + nestIndex++);
            }
        }

        this.outermost = pipelines.get(pipelines.size() - 1);
        this.innermost = pipelines.get(0);
    }
}
