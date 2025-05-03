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


import org.bithon.component.commons.expression.FunctionExpression;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.IdentifierExpression;
import org.bithon.component.commons.expression.LiteralExpression;
import org.bithon.component.commons.expression.MacroExpression;
import org.bithon.component.commons.expression.optimzer.AbstractOptimizer;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.datasource.query.Interval;
import org.bithon.server.datasource.query.ast.QueryStageFunctions;

import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 22/4/25 10:03 pm
 */
class MacroExpressionSubstitution extends AbstractOptimizer {
    private final Map<String, IExpression> macros;

    MacroExpressionSubstitution(Interval interval) {
        this.macros = Map.of("interval", LiteralExpression.ofLong(interval.getStep() == null ? interval.getTotalSeconds() : interval.getStep().getSeconds()),
                             "instanceCount", new FunctionExpression(new QueryStageFunctions.Cardinality(), new IdentifierExpression("instanceName")));
    }

    @Override
    public IExpression visit(MacroExpression expression) {
        IExpression replacement = macros.get(expression.getMacro());
        if (replacement == null) {
            throw new RuntimeException(StringUtils.format("variable (%s) not provided in context", expression.getMacro()));
        }
        return replacement;
    }
}
