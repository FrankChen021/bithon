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


import org.bithon.component.commons.expression.FunctionExpression;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.IdentifierExpression;
import org.bithon.component.commons.expression.MacroExpression;
import org.bithon.component.commons.expression.function.builtin.AggregateFunction;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.storage.datasource.query.ast.QueryStageFunctions;
import org.bithon.server.storage.jdbc.common.dialect.Expression2Sql;
import org.bithon.server.storage.jdbc.common.dialect.ISqlDialect;
import org.bithon.server.storage.metrics.Interval;

import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 12/4/25 9:57 am
 */
class Expression2SqlSerializer extends Expression2Sql {
    protected final Map<String, Object> variables;
    private long windowFunctionLength;

    Expression2SqlSerializer(ISqlDialect sqlDialect, Map<String, Object> variables, Interval interval) {
        super(null, sqlDialect);
        this.variables = variables;

        if (interval.getStep() != null) {
            windowFunctionLength = interval.getStep().getSeconds();
        } else {
            /**
             * For Window functions, since the timestamp of records might cross two windows,
             * we need to make sure the record in the given time range has only one window.
             */
            long endTime = interval.getEndTime().getMilliseconds();
            long startTime = interval.getStartTime().getMilliseconds();
            windowFunctionLength = (endTime - startTime) / 1000;
            while (startTime / windowFunctionLength != endTime / windowFunctionLength) {
                windowFunctionLength *= 2;
            }
        }
    }

    @Override
    public String serialize(IExpression expression) {
        // Apply optimization for different DBMS first
        sqlDialect.transform(null, expression).serializeToText(this);
        return sb.toString();
    }

    @Override
    public void serialize(MacroExpression expression) {
        Object variableValue = variables.get(expression.getMacro());
        if (variableValue == null) {
            throw new RuntimeException(StringUtils.format("variable (%s) not provided in context",
                                                          expression.getMacro()));
        }
        sb.append(variableValue);
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

        if (expression.getFunction() instanceof AggregateFunction.Last) {
            // Currently, only identifier expression is supported in the last aggregator
            String column = ((IdentifierExpression) expression.getArgs().get(0)).getIdentifier();
            sb.append(this.sqlDialect.lastAggregator(column, windowFunctionLength));
            return;
        }

        if (expression.getFunction() instanceof AggregateFunction.First) {
            // Currently, only identifier expression is supported in the first aggregator
            String column = ((IdentifierExpression) expression.getArgs().get(0)).getIdentifier();
            sb.append(this.sqlDialect.firstAggregator(column, windowFunctionLength));
            return;
        }

        super.serialize(expression);
    }
}
