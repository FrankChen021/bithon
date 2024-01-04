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

package org.bithon.server.storage.jdbc.h2;

import com.fasterxml.jackson.annotation.JsonTypeName;
import org.bithon.component.commons.expression.ComparisonExpression;
import org.bithon.component.commons.expression.FunctionExpression;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.IdentifierExpression;
import org.bithon.component.commons.expression.LiteralExpression;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.storage.common.expression.optimizer.ExpressionOptimizer;
import org.bithon.server.storage.datasource.builtin.Functions;
import org.bithon.server.storage.datasource.query.ast.SimpleAggregateExpressions;
import org.bithon.server.storage.jdbc.common.dialect.ISqlDialect;

import java.util.Arrays;

/**
 * @author Frank Chen
 * @date 17/4/23 11:20 pm
 */
@JsonTypeName("h2")
public class H2SqlDialect implements ISqlDialect {

    @Override
    public String quoteIdentifier(String identifier) {
        return "\"" + identifier + "\"";
    }

    @Override
    public String timeFloorExpression(IExpression timestampExpression, long interval) {
        return StringUtils.format("UNIX_TIMESTAMP(%s)/ %d * %d", timestampExpression.serializeToText(), interval, interval);
    }

    @Override
    public boolean groupByUseRawExpression() {
        return true;
    }

    @Override
    public boolean allowSameAggregatorExpression() {
        return true;
    }

    @Override
    public String stringAggregator(String field) {
        return StringUtils.format("group_concat(\"%s\")", field);
    }

    @Override
    public String firstAggregator(String field, String name, long window) {
        return StringUtils.format(
            "FIRST_VALUE(\"%s\") OVER (partition by %s ORDER BY \"timestamp\") AS \"%s\"",
            field,
            this.timeFloorExpression(new IdentifierExpression("timestamp"), window),
            name);
    }

    @Override
    public String lastAggregator(String field, long window) {
        // NOTE: use FIRST_VALUE instead of LAST_VALUE because the latter one returns the wrong result
        return StringUtils.format(
            "FIRST_VALUE(\"%s\") OVER (partition by %s ORDER BY \"timestamp\" DESC)",
            field,
            this.timeFloorExpression(new IdentifierExpression("timestamp"), window));
    }

    @Override
    public boolean useWindowFunctionAsAggregator(String aggregator) {
        return SimpleAggregateExpressions.FirstAggregateExpression.TYPE.equals(aggregator)
            || SimpleAggregateExpressions.LastAggregateExpression.TYPE.equals(aggregator);
    }

    @Override
    public IExpression transform(IExpression expression) {
        return expression.accept(new ExpressionOptimizer.AbstractOptimizer() {
            @Override
            public IExpression visit(FunctionExpression expression) {
                if ("startsWith".equals(expression.getName())) {
                    // H2 does not provide startsWith function, turns it into LIKE expression as: LIKE 'prefix%'
                    IExpression patternExpression = expression.getParameters().get(1);
                    if (patternExpression instanceof LiteralExpression) {
                        patternExpression = new LiteralExpression(((LiteralExpression) patternExpression).getValue() + "%");
                    } else {
                        patternExpression = new FunctionExpression(Functions.getInstance().getFunction("concat"),
                                                                   Arrays.asList(patternExpression, new LiteralExpression("%")));
                    }
                    return new ComparisonExpression.LIKE(expression.getParameters().get(0),
                                                         patternExpression);
                } else if ("endsWith".equals(expression.getName())) {
                    // H2 does not provide startsWith function, turns it into LIKE expression as: LIKE '%prefix'
                    IExpression patternExpression = expression.getParameters().get(1);
                    if (patternExpression instanceof LiteralExpression) {
                        patternExpression = new LiteralExpression("%" + ((LiteralExpression) patternExpression).getValue());
                    } else {
                        patternExpression = new FunctionExpression(Functions.getInstance().getFunction("concat"),
                                                                   Arrays.asList(new LiteralExpression("%"), patternExpression));
                    }
                    return new ComparisonExpression.LIKE(expression.getParameters().get(0),
                                                         patternExpression);
                }

                return expression;
            }
        });
    }
}
