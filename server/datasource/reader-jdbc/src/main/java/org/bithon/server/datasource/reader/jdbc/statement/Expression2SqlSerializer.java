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


import org.bithon.component.commons.expression.FunctionExpression;
import org.bithon.component.commons.expression.IdentifierExpression;
import org.bithon.server.datasource.reader.jdbc.dialect.Expression2Sql;
import org.bithon.server.datasource.reader.jdbc.dialect.ISqlDialect;
import org.bithon.server.datasource.reader.jdbc.statement.ast.QueryStageFunctions;

/**
 * @author frank.chen021@outlook.com
 * @date 12/4/25 9:57 am
 */
class Expression2SqlSerializer extends Expression2Sql {

    Expression2SqlSerializer(ISqlDialect sqlDialect) {
        super(null, sqlDialect);
    }

    @Override
    public void serialize(FunctionExpression expression) {
        if (expression.getFunction() instanceof QueryStageFunctions.Cardinality) {
            sb.append("count(distinct ");
            expression.getArgs().get(0).serializeToText(this);
            sb.append(")");
            return;
        }

        if (expression.getFunction() instanceof QueryStageFunctions.GroupConcat) {
            // Currently, only identifier expression is supported in the group concat aggregator
            String column = ((IdentifierExpression) expression.getArgs().get(0)).getIdentifier();
            sb.append(this.sqlDialect.stringAggregator(column));
            return;
        }

        super.serialize(expression);
    }
}
