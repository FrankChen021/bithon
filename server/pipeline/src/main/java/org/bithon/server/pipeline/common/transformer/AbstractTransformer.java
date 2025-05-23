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

package org.bithon.server.pipeline.common.transformer;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import org.bithon.component.commons.expression.ConditionalExpression;
import org.bithon.component.commons.expression.FunctionExpression;
import org.bithon.component.commons.expression.IDataType;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.LogicalExpression;
import org.bithon.component.commons.expression.function.Functions;
import org.bithon.component.commons.utils.Preconditions;
import org.bithon.server.datasource.expression.ExpressionASTBuilder;
import org.bithon.server.datasource.input.IInputRow;

public abstract class AbstractTransformer implements ITransformer {

    @JsonIgnore
    private final IExpression whereCondition;

    @Getter
    private final String where;

    protected AbstractTransformer(String where) {
        this.where = where;

        this.whereCondition = this.where == null ? null : ExpressionASTBuilder.builder().functions(Functions.getInstance()).build(this.where);
        if (this.whereCondition != null) {
            validateConditionExpression(whereCondition);
        }
    }

    public TransformResult transform(IInputRow inputRow) throws TransformException {
        if (whereCondition != null) {
            boolean satisfied = (boolean) whereCondition.evaluate(inputRow);
            if (!satisfied) {
                return TransformResult.CONTINUE;
            }
        }
        return transformInternal(inputRow);
    }

    protected abstract TransformResult transformInternal(IInputRow inputRow) throws TransformException;

    static void validateConditionExpression(IExpression expression) {
        Preconditions.checkIfTrue(expression instanceof LogicalExpression
                                  || expression instanceof ConditionalExpression
                                  || (expression instanceof FunctionExpression && expression.getDataType().equals(IDataType.BOOLEAN)),
                                  "The expression [%s] must be a LOGICAL/CONDITIONAL or a function expression that returns boolean",
                                  expression.serializeToText());
    }
}
