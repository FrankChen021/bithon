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

import org.bithon.component.commons.expression.FunctionExpression;
import org.bithon.component.commons.expression.MacroExpression;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.storage.datasource.query.ast.QueryStageFunctions;
import org.bithon.server.storage.jdbc.common.dialect.Expression2Sql;
import org.bithon.server.storage.jdbc.common.dialect.ISqlDialect;

import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2024/7/28 17:03
 */
class Expression2SqlSerializer extends Expression2Sql {
    protected final Map<String, Object> variables;

    Expression2SqlSerializer(ISqlDialect sqlDialect, Map<String, Object> variables) {
        super(null, sqlDialect);
        this.variables = variables;
    }

    @Override
    public boolean visit(MacroExpression expression) {
        Object variableValue = variables.get(expression.getMacro());
        if (variableValue == null) {
            throw new RuntimeException(StringUtils.format("variable (%s) not provided in context",
                                                          expression.getMacro()));
        }
        sb.append(variableValue);

        return false;
    }

    @Override
    public boolean visit(FunctionExpression expression) {
        if (expression.getFunction() instanceof QueryStageFunctions.Cardinality) {
            sb.append("count(distinct ");
            expression.getArgs().get(0).accept(this);
            sb.append(")");
            return false;
        }

        return super.visit(expression);
    }
}
