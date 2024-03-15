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

package org.bithon.server.pipeline.common.transform.transformer;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import org.bithon.component.commons.expression.ConditionalExpression;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.LogicalExpression;
import org.bithon.component.commons.utils.Preconditions;
import org.bithon.server.storage.common.expression.ExpressionASTBuilder;
import org.bithon.server.storage.datasource.builtin.Functions;
import org.bithon.server.storage.datasource.input.IInputRow;

public abstract class AbstractTransformer implements ITransformer {

    @JsonIgnore
    private final IExpression whereCondition;

    @Getter
    private final String where;

    protected AbstractTransformer(String where) {
        this.where = where;

        this.whereCondition = this.where == null ? null : ExpressionASTBuilder.builder().functions(Functions.getInstance()).build(this.where);
        Preconditions.checkIfTrue(this.whereCondition == null || this.whereCondition instanceof LogicalExpression || this.whereCondition instanceof ConditionalExpression,
                                  "The 'where' property must be a LOGICAL or CONDITIONAL expression");
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
}
