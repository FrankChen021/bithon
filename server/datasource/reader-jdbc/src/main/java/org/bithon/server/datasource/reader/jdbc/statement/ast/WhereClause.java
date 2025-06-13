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

import lombok.Getter;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.LogicalExpression;
import org.bithon.server.datasource.query.ast.IASTNode;

import java.util.ArrayList;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2022/9/4 14:55
 */
@Getter
public class WhereClause implements IASTNode {

    private final List<IExpression> expressions = new ArrayList<>();

    public WhereClause and(IExpression expression) {
        if (expression == null) {
            return this;
        }

        if (expression instanceof LogicalExpression.AND andExpression) {
            // Flatten the AND expression
            for (IExpression expr : andExpression.getOperands()) {
                addExpression(expr);
            }
        } else {
            expressions.add(expression);
        }
        return this;
    }

    public WhereClause and(IExpression... expressions) {
        if (expressions == null) {
            return this;
        }

        for (IExpression expression : expressions) {
            if (expression == null) {
                continue;
            }

            addExpression(expression);
        }
        return this;
    }

    private void addExpression(IExpression expr) {
        // Check if the expression already exists in the list
        if (this.expressions.stream().anyMatch(e -> e.equals(expr))) {
            return;
        }
        this.expressions.add(expr);
    }

    public boolean isEmpty() {
        return expressions.isEmpty();
    }
}
